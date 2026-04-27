package com.proxio.backend.services;

import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.exceptions.DeleteOperationException;
import com.proxio.backend.exceptions.ResourceNotFoundException;
import com.proxio.backend.exceptions.UpdateOperationException;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.repositories.UserRatingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRatingService {

    private final UserRatingRepository userRatingRepository;

    public UserRatingService(UserRatingRepository userRatingRepository) {
        this.userRatingRepository = userRatingRepository;
    }

    public UserRating create(UserRating userRating) {
        try {
            return userRatingRepository.save(userRating);
        } catch (Exception exception) {
            throw new CreateOperationException("Could not create UserRating.");
        }
    }

    public List<UserRating> getAll() {
        return userRatingRepository.findAll();
    }

    public UserRating getById(Long id) {
        return userRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserRating with id " + id + " was not found."));
    }

    public UserRating update(Long id, UserRating userRating) {
        try {
            UserRating existing = getById(id);
        existing.setRater(userRating.getRater());
        existing.setRated(userRating.getRated());
        existing.setOrder(userRating.getOrder());
        existing.setModeratedBy(userRating.getModeratedBy());
        existing.setScore(userRating.getScore());
        existing.setComment(userRating.getComment());
        existing.setStatus(userRating.getStatus());
        existing.setModeratedAt(userRating.getModeratedAt());
            return userRatingRepository.save(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UpdateOperationException("Could not update UserRating with id " + id + ".");
        }
    }

    public void delete(Long id) {
        try {
            UserRating existing = getById(id);
            userRatingRepository.delete(existing);
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeleteOperationException("Could not delete UserRating with id " + id + ".");
        }
    }
}
