DROP TABLE IF EXISTS resource_unavailability CASCADE;

CREATE TABLE resource_unavailability (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         user_id BIGINT NOT NULL,
                                         unavailability_date DATE NOT NULL,
                                         start_time TIME NOT NULL,
                                         end_time TIME NOT NULL,
                                         reason VARCHAR(255),
                                         CONSTRAINT fk_ru_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE weekly_availability (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     day_of_week VARCHAR(20) NOT NULL,
                                     start_time TIME NOT NULL,
                                     end_time TIME NOT NULL,
                                     CONSTRAINT fk_wa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);