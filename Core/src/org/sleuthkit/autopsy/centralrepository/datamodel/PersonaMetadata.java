/*
 * Central Repository
 *
 * Copyright 2020 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.centralrepository.datamodel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.sleuthkit.datamodel.SleuthkitCase;

/**
 * This class abstracts metadata associated with a Persona.
 * Metadata is in the form of a name/value pair.
 * 
 * A Persona may have zero or more metadata.
 * 
 */
public class PersonaMetadata {
    
    private final long personaId;
    private final String name;
    private final String value;
    private final String justification;
    private final Persona.Confidence confidence;
    private final long dateAdded;
    private final CentralRepoExaminer examiner;

    public long getPersonaId() {
        return personaId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getJustification() {
        return justification;
    }

    public Persona.Confidence getConfidence() {
        return confidence;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public CentralRepoExaminer getExaminer() {
        return examiner;
    }
    
    public PersonaMetadata(long personaId, String name, String value, String justification, Persona.Confidence confidence, long dateAdded, CentralRepoExaminer examiner) {
        this.personaId = personaId;
        this.name = name;
        this.value = value;
        this.justification = justification;
        this.confidence = confidence;
        this.dateAdded = dateAdded;
        this.examiner = examiner;
    }
    
     /**
     * Adds specified metadata to the given persona.
     *
     * @param personaId Id of persona to add metadata for.
     * @param name Metadata name.
     * @param value Metadata value.
     * @param justification Reason for adding the metadata, may be null.
     * @param confidence Confidence level.
     *
     * @return PersonaMetadata
     * @throws CentralRepoException If there is an error in adding metadata.
     */
    static PersonaMetadata addPersonaMetadata(long personaId, String name, String value, String justification, Persona.Confidence confidence) throws CentralRepoException {

        CentralRepoExaminer examiner = CentralRepository.getInstance().getOrInsertExaminer(System.getProperty("user.name"));

        Instant instant = Instant.now();
        Long timeStampMillis = instant.toEpochMilli();

        String insertClause = " INTO persona_metadata (persona_id, name, value, justification, confidence_id, date_added, examiner_id ) "
                + "VALUES ( "
                + personaId + ", "
                + "'" + name + "', "
                + "'" + value + "', "
                + "'" + ((StringUtils.isBlank(justification) ? "" : SleuthkitCase.escapeSingleQuotes(justification))) + "', "
                + confidence.getLevelId() + ", "
                + timeStampMillis.toString() + ", "
                + examiner.getId()
                + ")";

        CentralRepository.getInstance().executeInsertSQL(insertClause);
        return new PersonaMetadata(personaId, name, value, justification, confidence, timeStampMillis, examiner);
    }
    
    /**
     * Callback to process a Persona metadata query.
     */
    private static class PersonaMetadataQueryCallback implements CentralRepositoryDbQueryCallback {

        Collection<PersonaMetadata> personaMetadataList = new ArrayList<>();

        @Override
        public void process(ResultSet rs) throws SQLException {

            while (rs.next()) {
                CentralRepoExaminer examiner = new CentralRepoExaminer(
                        rs.getInt("examiner_id"),
                        rs.getString("login_name"));

                PersonaMetadata metaData = new PersonaMetadata(
                        rs.getLong("persona_id"),
                        rs.getString("name"),
                        rs.getString("value"),
                        rs.getString("justification"),
                        Persona.Confidence.fromId(rs.getInt("confidence_id")),
                        Long.parseLong(rs.getString("date_added")),
                        examiner);

                personaMetadataList.add(metaData);
            }
        }

        Collection<PersonaMetadata> getMetadataList() {
            return Collections.unmodifiableCollection(personaMetadataList);
        }
    };
    
     /**
     * Gets all metadata for the persona with specified id.
     *
     * @param personaId Id of the persona for which to get the metadata.
     * @return A collection of metadata, may be empty.
     *
     * @throws CentralRepoException If there is an error in retrieving aliases.
     */
    static Collection<PersonaMetadata> getPersonaMetadata(long personaId) throws CentralRepoException {
        String queryClause = "SELECT pmd.id, pmd.persona_id, pmd.name, pmd.value, pmd.justification, pmd.confidence_id, pmd.date_added, pmd.examiner_id, e.login_name, e.display_name "
                + "FROM persona_metadata as pmd "
                + "INNER JOIN examiners as e ON e.id = pmd.examiner_id ";

        PersonaMetadataQueryCallback queryCallback = new PersonaMetadataQueryCallback();
        CentralRepository.getInstance().executeSelectSQL(queryClause, queryCallback);

        return queryCallback.getMetadataList();

    }
    
}
