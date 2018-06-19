/*
 * Copyright 2016 Crown Copyright
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

package stroom.streamstore.shared;

import stroom.entity.shared.Action;
import stroom.entity.shared.ResultList;
import stroom.streamstore.meta.api.FindStreamCriteria;
import stroom.streamstore.meta.api.StreamDataRow;

public class FindStreamAction extends Action<ResultList<StreamDataRow>> {
    private static final long serialVersionUID = -3560107233301674555L;

    private FindStreamCriteria criteria;

    public FindStreamAction() {
    }

    public FindStreamAction(final FindStreamCriteria criteria) {
        this.criteria = criteria;
    }

    public FindStreamCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(FindStreamCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public String getTaskName() {
        return "Find Stream Action";
    }
}
