/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.model.api.hooks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import java.util.Collection;
import java.util.List;

/**
 * TODO not finished yet, not usable
 *
 * @author lazyman
 */
public interface ReadHook {

//    <T extends ObjectType> List<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
//                                                              Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException,
//            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException;
//
//    <T extends ObjectType> void searchObjectsIterative(Class<T> type, ObjectQuery query,
//                                                       ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;
//
//    <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
//                                                    Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException,
//            CommunicationException, ConfigurationException;


    <T extends ObjectType> void invoke(PrismObject<T> object, Collection<SelectorOptions<GetOperationOptions>> options,
                                                    Task task, OperationResult parentResult);
}