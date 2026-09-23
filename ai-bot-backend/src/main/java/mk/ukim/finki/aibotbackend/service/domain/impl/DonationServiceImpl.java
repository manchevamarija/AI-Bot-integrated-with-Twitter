package mk.ukim.finki.aibotbackend.service.domain.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.aibotbackend.integration.vezilka.DonationReceipt;
import mk.ukim.finki.aibotbackend.integration.vezilka.TextDonationRequest;
import mk.ukim.finki.aibotbackend.integration.vezilka.VezilkaClient;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import mk.ukim.finki.aibotbackend.model.domain.DonationBatch;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.exception.DonationBatchNotFoundException;
import mk.ukim.finki.aibotbackend.model.exception.InvalidDonationStateException;
import mk.ukim.finki.aibotbackend.repository.DonationBatchRepository;
import mk.ukim.finki.aibotbackend.service.domain.DonationService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DonationServiceImpl implements DonationService {
    private final DonationBatchRepository donationBatchRepository;
    private final ExtractedPostService extractedPostService;
    private final VezilkaClient vezilkaClient;

    public DonationServiceImpl(
        DonationBatchRepository donationBatchRepository,
        ExtractedPostService extractedPostService,
        VezilkaClient vezilkaClient
    ) {
        this.donationBatchRepository = donationBatchRepository;
        this.extractedPostService = extractedPostService;
        this.vezilkaClient = vezilkaClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationBatch> findAll() {
        return donationBatchRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DonationBatch> findById(Long id) {
        return donationBatchRepository.findById(id);
    }

    @Override
    @Transactional
    public DonationBatch createBatch(List<Long> postIds) {
        Set<Long> uniquePostIds = new LinkedHashSet<>(postIds);
        List<ExtractedPost> posts = extractedPostService.findAllById(
            uniquePostIds.stream().toList()
        );
        validatePostsForDonation(uniquePostIds, posts);

        DonationBatch batch = donationBatchRepository.save(
            new DonationBatch(DonationStatus.DRAFT)
        );
        for (ExtractedPost post : posts) {
            post.setDonationBatch(batch);
            batch.getPosts().add(post);
        }
        extractedPostService.saveAll(posts);
        return batch;
    }

    @Override
    @Transactional
    public DonationBatch approve(Long id) {
        DonationBatch batch = requireBatch(id);
        requireStatus(batch, DonationStatus.DRAFT);
        batch.setStatus(DonationStatus.APPROVED);
        return donationBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public DonationBatch submit(Long id) {
        DonationBatch batch = requireBatch(id);
        requireStatus(batch, DonationStatus.APPROVED);
        if (batch.getPosts().isEmpty()) {
            throw new IllegalArgumentException("A donation batch must contain at least one post");
        }

        // The Public Donation API takes one item per call, so a multi-post batch
        // becomes one submission per post. A duplicate item still means the
        // content is in the corpus, so it counts as a success alongside "accepted".
        List<DonationReceipt> receipts = batch.getPosts().stream()
            .map(post -> vezilkaClient.submitTextDonation(buildDonationRequest(post)))
            .toList();
        List<DonationReceipt> successful = receipts.stream()
            .filter(DonationReceipt::accepted)
            .toList();

        long duplicates = receipts.stream().filter(DonationReceipt::deduped).count();
        log.info(
            "Vezilka donation batch {}: {} submitted, {} accepted ({} duplicates), {} rejected",
            batch.getId(), receipts.size(), successful.size(), duplicates,
            receipts.size() - successful.size()
        );
        receipts.stream()
            .filter(receipt -> !receipt.accepted())
            .forEach(receipt -> log.info("Vezilka rejected an item: {}", receipt.rejectionReason()));

        batch.setVezilkaReference(compactReference(successful.isEmpty() ? receipts : successful));
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setStatus(successful.isEmpty() ? DonationStatus.REJECTED : DonationStatus.SUBMITTED);
        return donationBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void refreshSubmittedStatuses() {
        List<DonationBatch> submittedBatches =
            donationBatchRepository.findAllByStatus(DonationStatus.SUBMITTED);

        for (DonationBatch batch : submittedBatches) {
            try {
                DonationStatus status = vezilkaClient.checkStatus(batch.getVezilkaReference());
                if (status != null && status != batch.getStatus()) {
                    batch.setStatus(status);
                    donationBatchRepository.save(batch);
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Could not refresh Vezilka status for donation batch {}",
                    batch.getId(),
                    exception
                );
            }
        }
    }

    /**
     * The reference column holds 255 characters, so a large batch cannot keep
     * every item id. It keeps the first id (enough to re-check the batch) and
     * how many more items there were, e.g. "9f2c1e8a-... (+73)".
     */
    private String compactReference(List<DonationReceipt> receipts) {
        List<String> ids = receipts.stream()
            .map(DonationReceipt::reference)
            .filter(Objects::nonNull)
            .toList();
        if (ids.isEmpty()) {
            return null;
        }
        return ids.size() == 1 ? ids.getFirst() : ids.getFirst() + " (+" + (ids.size() - 1) + ")";
    }

    private TextDonationRequest buildDonationRequest(ExtractedPost post) {
        LocalDateTime retrievedAt = post.getPostedAt() != null ? post.getPostedAt() : post.getCreatedAt();
        return new TextDonationRequest(
            post.getSourceUrl(),
            post.getContent(),
            post.getAuthorHandle() == null ? null : "@" + post.getAuthorHandle(),
            retrievedAt == null ? null : retrievedAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

    private void validatePostsForDonation(Set<Long> requestedIds, List<ExtractedPost> posts) {
        if (posts.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Every selected post must exist");
        }
        if (posts.stream().anyMatch(post -> post.getDonationBatch() != null)) {
            throw new IllegalArgumentException("Already donated posts cannot be selected again");
        }
    }

    private DonationBatch requireBatch(Long id) {
        return donationBatchRepository.findById(id)
            .orElseThrow(() -> new DonationBatchNotFoundException(id));
    }

    private void requireStatus(DonationBatch batch, DonationStatus requiredStatus) {
        if (batch.getStatus() != requiredStatus) {
            throw new InvalidDonationStateException(batch.getId(), batch.getStatus());
        }
    }
}
