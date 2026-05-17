-- V2: Add sign method field and application form extension fields
ALTER TABLE activity ADD COLUMN sign_method VARCHAR(50) DEFAULT 'gps,scan,photo' COMMENT 'Allowed sign-in methods, comma separated';

ALTER TABLE volunteer_record
  ADD COLUMN applicant_name VARCHAR(50) COMMENT 'Applicant name',
  ADD COLUMN applicant_phone VARCHAR(20) COMMENT 'Applicant phone',
  ADD COLUMN applicant_email VARCHAR(100) COMMENT 'Applicant email',
  ADD COLUMN emergency_contact VARCHAR(50) COMMENT 'Emergency contact',
  ADD COLUMN emergency_phone VARCHAR(20) COMMENT 'Emergency contact phone',
  ADD COLUMN remarks VARCHAR(500) COMMENT 'Remarks';
