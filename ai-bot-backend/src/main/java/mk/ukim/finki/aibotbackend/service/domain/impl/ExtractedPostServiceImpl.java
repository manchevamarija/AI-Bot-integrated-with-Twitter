package mk.ukim.finki.aibotbackend.service.domain.impl;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.repository.ExtractedPostRepository;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractedPostServiceImpl implements ExtractedPostService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TOP_SIZE = 50;

    private final ExtractedPostRepository extractedPostRepository;

    public ExtractedPostServiceImpl(ExtractedPostRepository extractedPostRepository) {
        this.extractedPostRepository = extractedPostRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExtractedPost> findAll(PostFilterDto filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Specification<ExtractedPost> specification = buildSpecification(filter);
        PageRequest pageRequest = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return extractedPostRepository.findAll(specification, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtractedPost> findById(Long id) {
        return extractedPostRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractedPost> findAllById(List<Long> ids) {
        return extractedPostRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractedPost> findAllBySessionId(Long sessionId) {
        return extractedPostRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    @Override
    @Transactional
    public List<ExtractedPost> saveAll(List<ExtractedPost> posts) {
        return extractedPostRepository.saveAll(posts);
    }

    @Override
    @Transactional
    public Optional<ExtractedPost> deleteById(Long id) {
        Optional<ExtractedPost> post = extractedPostRepository.findById(id);
        post.ifPresent(extractedPostRepository::delete);
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractedPost> findTop(PostFilterDto filter, String attribute, int limit) {
        Specification<ExtractedPost> known = (root, query, criteriaBuilder) ->
            criteriaBuilder.isNotNull(root.get(attribute));
        PageRequest pageRequest = PageRequest.of(
            0,
            Math.clamp(limit, 1, MAX_TOP_SIZE),
            Sort.by(Sort.Direction.DESC, attribute).and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return extractedPostRepository
            .findAll(buildSpecification(filter).and(known), pageRequest)
            .getContent();
    }

    private Specification<ExtractedPost> buildSpecification(PostFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.sessionId() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("session").get("id"),
                    filter.sessionId()
                ));
            }
            if (filter.socialNetwork() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("session").get("socialNetwork"),
                    filter.socialNetwork()
                ));
            }
            if (filter.minMacedonianConfidence() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("macedonianConfidence"),
                    filter.minMacedonianConfidence()
                ));
            }
            if (filter.donated() != null) {
                predicates.add(filter.donated()
                    ? criteriaBuilder.isNotNull(root.get("donationBatch"))
                    : criteriaBuilder.isNull(root.get("donationBatch")));
            }
            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("authorHandle")), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
