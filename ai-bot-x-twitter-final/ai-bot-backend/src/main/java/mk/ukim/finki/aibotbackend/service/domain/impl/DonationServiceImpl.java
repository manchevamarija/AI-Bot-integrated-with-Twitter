package mk.ukim.finki.aibotbackend.service.domain.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.aibotbackend.integration.vezilka.DonationReceipt;
import mk.ukim.finki.aibotbackend.integration.vezilka.TextDonationRequest;
import mk.ukim.finki.aibotbackend.integration.vezilka.VezilkaClient;
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
    private static final String DONATION_TITLE = "Македонски објави од X";
    private static final String POST_SEPARATOR = "\n\n---\n\n";

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

        DonationReceipt receipt = vezilkaClient.submitTextDonation(
            buildDonationRequest(batch)
        );
        batch.setVezilkaReference(receipt.reference());
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setStatus(DonationStatus.SUBMITTED);
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

    private TextDonationRequest buildDonationRequest(DonationBatch batch) {
        String content = batch.getPosts().stream()
            .map(post -> {
                String source = post.getSourceUrl() == null
                    ? ""
                    : "\nИзвор: " + post.getSourceUrl();
                return post.getContent() + source;
            })
            .collect(Collectors.joining(POST_SEPARATOR));

        String sources = batch.getPosts().stream()
            .map(ExtractedPost::getSourceUrl)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(", "));

        return new TextDonationRequest(DONATION_TITLE, content, sources);
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
