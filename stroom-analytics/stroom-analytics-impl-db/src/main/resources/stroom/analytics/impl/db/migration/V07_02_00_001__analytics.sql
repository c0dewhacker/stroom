-- ------------------------------------------------------------------------
-- Copyright 2020 Crown Copyright
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- ------------------------------------------------------------------------

-- Stop NOTE level warnings about objects (not)? existing
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;

-- --------------------------------------------------

--
-- Create the table
--
CREATE TABLE IF NOT EXISTS `analytic_process` (
  `uuid` varchar(255) NOT NULL,
  `version` int NOT NULL,
  `create_time_ms` bigint NOT NULL,
  `create_user` varchar(255) NOT NULL,
  `update_time_ms` bigint NOT NULL,
  `update_user` varchar(255) NOT NULL,
  `analytic_uuid` varchar(255) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `node` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `analytic_process_analytic_uuid_idx` (`analytic_uuid`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `analytic_process_tracker` (
  `fk_analytic_process_uuid` varchar(255) NOT NULL,
  `data` longtext DEFAULT NULL,
  PRIMARY KEY (`fk_analytic_process_uuid`),
  CONSTRAINT `fk_analytic_process_uuid`
    FOREIGN KEY (`fk_analytic_process_uuid`)
    REFERENCES `analytic_process` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- --------------------------------------------------

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=2 tabstop=2 expandtab:
