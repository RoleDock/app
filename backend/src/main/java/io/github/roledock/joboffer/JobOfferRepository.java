package io.github.roledock.joboffer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JobOfferRepository extends JpaRepository<JobOffer, UUID> {}
