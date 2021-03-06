/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate

/**
 * We should use delegate context for DI in old frontend. For the FIR we don't have context delegate, so we should use parent class.
 */
class JvmTypeSpecificityComparatorDelegate(override val context: TypeSystemInferenceExtensionContextDelegate) : JvmTypeSpecificityComparator(context)

open class JvmTypeSpecificityComparator(open val context: TypeSystemInferenceExtensionContext) : TypeSpecificityComparator {

    override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean = with(context) {
        val simpleGeneral = general.asSimpleType()
        if (!specific.isFlexible() || simpleGeneral == null) return false

        // general is inflexible
        val flexibility = specific.asFlexibleType()!!

        // For primitive types we have to take care of the case when there are two overloaded methods like
        //    foo(int) and foo(Integer)
        // if we do not discriminate one of them, any call to foo(kotlin.Int) will result in overload resolution ambiguity
        // so, for such cases, we discriminate Integer in favour of int
        if (!simpleGeneral.isPrimitiveType() || !flexibility.lowerBound().isPrimitiveType()) {
            return false
        }

        // Int? >< Int!
        if (simpleGeneral.isMarkedNullable()) return false
        // Int! lessSpecific Int
        return true
    }
}