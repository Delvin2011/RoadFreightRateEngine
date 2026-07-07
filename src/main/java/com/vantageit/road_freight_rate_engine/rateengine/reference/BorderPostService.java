package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.BorderPostRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.BorderPostRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.BorderPostResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BorderPostService {

    private final BorderPostRepository borderPostRepository;

    public BorderPostService(BorderPostRepository borderPostRepository) {
        this.borderPostRepository = borderPostRepository;
    }

    public BorderPostResponse create(BorderPostRequest request) {
        BorderPost borderPost = BorderPost.builder()
                .code(request.code())
                .name(request.name())
                .originCountry(request.originCountry())
                .destinationCountry(request.destinationCountry())
                .build();
        return toResponse(borderPostRepository.save(borderPost));
    }

    @Transactional(readOnly = true)
    public List<BorderPostResponse> getAll() {
        return borderPostRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BorderPostResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public BorderPostResponse update(UUID id, BorderPostRequest request) {
        BorderPost borderPost = findOrThrow(id);
        borderPost.setCode(request.code());
        borderPost.setName(request.name());
        borderPost.setOriginCountry(request.originCountry());
        borderPost.setDestinationCountry(request.destinationCountry());
        return toResponse(borderPostRepository.save(borderPost));
    }

    public void delete(UUID id) {
        borderPostRepository.delete(findOrThrow(id));
    }

    private BorderPost findOrThrow(UUID id) {
        return borderPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BorderPost", id));
    }

    private BorderPostResponse toResponse(BorderPost borderPost) {
        return new BorderPostResponse(borderPost.getId(), borderPost.getCode(), borderPost.getName(),
                borderPost.getOriginCountry(), borderPost.getDestinationCountry());
    }
}
