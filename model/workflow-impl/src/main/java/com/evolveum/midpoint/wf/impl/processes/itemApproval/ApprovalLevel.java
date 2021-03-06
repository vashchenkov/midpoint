/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LevelEvaluationStrategyType;

import java.util.List;

/**
 * @author mederly
 */
public interface ApprovalLevel {
    String getName();

    String getDescription();

    List<? extends LightweightObjectRef> getApproverRefs();

    List<ExpressionType> getApproverExpressions();

    LevelEvaluationStrategyType getEvaluationStrategy();

    ExpressionType getAutomaticallyApproved();

    PrismContext getPrismContext();

    void setPrismContext(PrismContext prismContext);

    ApprovalLevelType toApprovalLevelType(PrismContext prismContext);
}
