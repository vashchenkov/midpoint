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
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.RetrieveOption;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class MiscSchemaUtil {
	
	private static Random rnd = new Random();
	
	public static ObjectListType toObjectListType(List<PrismObject<? extends ObjectType>> list) {
		ObjectListType listType = new ObjectListType();
		for (PrismObject<? extends ObjectType> o : list) {
			listType.getObject().add(o.asObjectable());
		}
		return listType;
	}
	
	public static <T extends ObjectType> List<PrismObject<T>> toList(Class<T> type, ObjectListType listType) {
		List<PrismObject<T>> list = new ArrayList<PrismObject<T>>();
		for (ObjectType o : listType.getObject()) {
			list.add(((T)o).asPrismObject());
		}
		return list;
	}
	
	public static <T extends ObjectType> List<T> toObjectableList(List<PrismObject<T>> objectList) {
		if (objectList == null) {
			return null;
		}
		List<T> objectableList = new ArrayList<T>(objectList.size());
		for (PrismObject<T> object: objectList) {
			objectableList.add(object.asObjectable());
		}
		return objectableList;
	}
	
	public static ImportOptionsType getDefaultImportOptions() {
		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(false);
		options.setValidateStaticSchema(false);
		options.setValidateDynamicSchema(false);
		options.setEncryptProtectedValues(true);
		options.setFetchResourceSchema(false);
		options.setSummarizeErrors(true);
		options.setSummarizeSucceses(true);
		return options;
	}

	public static CachingMetadataType generateCachingMetadata() {
		CachingMetadataType cmd = new CachingMetadataType();
		XMLGregorianCalendar xmlGregorianCalendarNow = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		cmd.setRetrievalTimestamp(xmlGregorianCalendarNow);
		cmd.setSerialNumber(generateSerialNumber());
		return cmd;
	}

	private static String generateSerialNumber() {
		return Long.toHexString(rnd.nextLong())+"-"+Long.toHexString(rnd.nextLong());
	}

	public static boolean isNullOrEmpty(ProtectedStringType ps) {
		return (ps == null || ps.isEmpty());
	}

	public static void setPassword(CredentialsType credentials, ProtectedStringType password) {
		PasswordType credPass = credentials.getPassword();
		if (credPass == null) {
			credPass = new PasswordType();
			credentials.setPassword(credPass);
		}
		credPass.setValue(password);
	}

	public static Collection<String> toCollection(String entry) {
		List<String> list = new ArrayList<String>(1);
		list.add(entry);
		return list;
	}

	public static Collection<ItemPath> itemReferenceListTypeToItemPathList(PropertyReferenceListType resolve) {
		Collection<ItemPath> itemPathList = new ArrayList<ItemPath>(resolve.getProperty().size());
		for (ItemPathType itemXPathElement: resolve.getProperty()) {
			itemPathList.add(itemXPathElement.getItemPath());
		}
		return itemPathList;
	}
	
	public static SelectorQualifiedGetOptionsType optionsToOptionsType(Collection<SelectorOptions<GetOperationOptions>> options){
		SelectorQualifiedGetOptionsType optionsType = new SelectorQualifiedGetOptionsType();
		List<SelectorQualifiedGetOptionType> retval = new ArrayList<>();
		for (SelectorOptions<GetOperationOptions> option: options){
			retval.add(selectorOptionToSelectorQualifiedGetOptionType(option));
		}
		optionsType.getOption().addAll(retval);
		return optionsType;
	}
	
	private static SelectorQualifiedGetOptionType selectorOptionToSelectorQualifiedGetOptionType(SelectorOptions<GetOperationOptions> selectorOption){
		ObjectSelectorType selectorType = selectorToSelectorType(selectorOption.getSelector());
		GetOperationOptionsType getOptionsType = getOptionsToGetOptionsType(selectorOption.getOptions());
		SelectorQualifiedGetOptionType selectorOptionType = new SelectorQualifiedGetOptionType();
		selectorOptionType.setOptions(getOptionsType);
		selectorOptionType.setSelector(selectorType);
		return selectorOptionType;
	}

	 private static ObjectSelectorType selectorToSelectorType(ObjectSelector selector) {
			if (selector == null) {
				return null;
			}
			ObjectSelectorType selectorType = new ObjectSelectorType();
			selectorType.setPath(new ItemPathType(selector.getPath()));
			return selectorType;
		}
	 
	 private static GetOperationOptionsType getOptionsToGetOptionsType(GetOperationOptions options) {
			GetOperationOptionsType optionsType = new GetOperationOptionsType();
			optionsType.setRetrieve(RetrieveOption.toRetrieveOptionType(options.getRetrieve()));
			optionsType.setResolve(options.getResolve());
			optionsType.setNoFetch(options.getNoFetch());
			optionsType.setRaw(options.getRaw());
			optionsType.setNoDiscovery(options.getDoNotDiscovery());
			return optionsType;
		}
   
	 public static List<SelectorOptions<GetOperationOptions>> optionsTypeToOptions(SelectorQualifiedGetOptionsType objectOptionsType) {
        if (objectOptionsType == null) {
            return null;
        }
        List<SelectorOptions<GetOperationOptions>> retval = new ArrayList<>();
        for (SelectorQualifiedGetOptionType optionType : objectOptionsType.getOption()) {
            retval.add(selectorQualifiedGetOptionTypeToSelectorOption(optionType));
        }
        return retval;
    }

	private static SelectorOptions<GetOperationOptions> selectorQualifiedGetOptionTypeToSelectorOption(SelectorQualifiedGetOptionType objectOptionsType) {
		ObjectSelector selector = selectorTypeToSelector(objectOptionsType.getSelector());
		GetOperationOptions options = getOptionsTypeToGetOptions(objectOptionsType.getOptions());
		return new SelectorOptions<>(selector, options);
	}

	private static GetOperationOptions getOptionsTypeToGetOptions(GetOperationOptionsType optionsType) {
		GetOperationOptions options = new GetOperationOptions();
        options.setRetrieve(RetrieveOption.fromRetrieveOptionType(optionsType.getRetrieve()));
        options.setResolve(optionsType.isResolve());
        options.setNoFetch(optionsType.isNoFetch());
        options.setRaw(optionsType.isRaw());
        options.setDoNotDiscovery(optionsType.isNoDiscovery());
		return options;
	}

    private static ObjectSelector selectorTypeToSelector(ObjectSelectorType selectorType) {
		if (selectorType == null) {
			return null;
		}
		return new ObjectSelector(selectorType.getPath().getItemPath());
	}
	
    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Collection<ObjectDelta<? extends ObjectType>> createCollection(ObjectDelta<?>... deltas) {
    	return (Collection)MiscUtil.createCollection(deltas);
    }
    
    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Collection<? extends ItemDelta<?,?>> createCollection(ItemDelta<?,?>... deltas) {
    	return (Collection)MiscUtil.createCollection(deltas);
    }
    
	public static Collection<ObjectDelta<? extends ObjectType>> cloneObjectDeltaCollection(
			Collection<ObjectDelta<? extends ObjectType>> origCollection) {
		if (origCollection == null) {
			return null;
		}
		Collection<ObjectDelta<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDelta<? extends ObjectType>>(origCollection.size());
		for (ObjectDelta<? extends ObjectType> origDelta: origCollection) {
			clonedCollection.add(origDelta.clone());
		}
		return clonedCollection;
	}
	
	public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneObjectDeltaOperationCollection(
			Collection<ObjectDeltaOperation<? extends ObjectType>> origCollection) {
		if (origCollection == null) {
			return null;
		}
		Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>(origCollection.size());
		for (ObjectDeltaOperation<? extends ObjectType> origDelta: origCollection) {
			clonedCollection.add(origDelta.clone());
		}
		return clonedCollection;
	}

    public static ObjectReferenceType createObjectReference(String oid, QName type) {
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        ref.setType(type);
        return ref;
    }
    
    public static boolean equalsIntent(String intent1, String intent2) {
		if (intent1 == null) {
			intent1 = SchemaConstants.INTENT_DEFAULT;
		}
		if (intent2 == null) {
			intent2 = SchemaConstants.INTENT_DEFAULT;
		}
		return intent1.equals(intent2);
	}
    
	public static boolean matchesKind(ShadowKindType expectedKind, ShadowKindType actualKind) {
		if (expectedKind == null) {
			return true;
		}
		return expectedKind.equals(actualKind);
	}


	public static AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(
			ProjectionPolicyType accountSynchronizationSettings) {
		if (accountSynchronizationSettings == null) {
			// default
			return AssignmentPolicyEnforcementType.RELATIVE;
		}
		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = accountSynchronizationSettings.getAssignmentPolicyEnforcement();
		if (assignmentPolicyEnforcement == null) {
			return AssignmentPolicyEnforcementType.RELATIVE;
		}
		return assignmentPolicyEnforcement;
	}

	public static boolean compareRelation(QName a, QName b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	public static PrismReferenceValue objectReferenceTypeToReferenceValue(ObjectReferenceType refType) {
		if (refType == null) {
			return null;
		}
		PrismReferenceValue rval = new PrismReferenceValue();
		rval.setOid(refType.getOid());
		rval.setDescription(refType.getDescription());
		rval.setFilter(refType.getFilter());
		rval.setRelation(refType.getRelation());
		rval.setTargetType(refType.getType());
		return rval;
	}
	
	public static PropertyLimitationsType getLimitationsType(List<PropertyLimitationsType> limitationsTypes, LayerType layer) throws SchemaException {
		if (limitationsTypes == null) {
			return null;
		}
		PropertyLimitationsType found = null;
		for (PropertyLimitationsType limitType: limitationsTypes) {
			if (contains(limitType.getLayer(),layer)) {
				if (found == null) {
					found = limitType;
				} else {
					throw new SchemaException("Duplicate definition of limitations for layer '"+layer+"'");
				}
			}
		}
		return found;
	}
	
	private static boolean contains(List<LayerType> layers, LayerType layer) {
		if (layers == null || layers.isEmpty()) {
			if (layer == null) {
				return true;
			} else {
				return false;
			}
		}
		return layers.contains(layer);
	}

	// Some searches may return duplicate objects. This is an utility method to remove the duplicates.
	public static <O extends ObjectType> void reduceSearchResult(List<PrismObject<O>> results) {
		if (results == null || results.isEmpty()) {
			return;
		}
		Map<String,PrismObject<O>> map = new HashMap<>();
		Iterator<PrismObject<O>> iterator = results.iterator();
		while (iterator.hasNext()) {
			PrismObject<O> prismObject = iterator.next();
			if (map.containsKey(prismObject.getOid())) {
				iterator.remove();
			} else {
				map.put(prismObject.getOid(), prismObject);
			}
		}
	}

}
