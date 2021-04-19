package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.util.UUID

@Repository
interface AuditRepository : CrudRepository<AuditEvent, UUID>
