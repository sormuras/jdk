/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.gc.g1;

import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

// Mirror class for G1HeapRegionType. Currently we don't actually include
// any of its fields but only iterate over it.

public class G1HeapRegionType extends VMObject {

    private static int freeTag;
    private static int youngMask;
    private static int edenTag;
    private static int survTag;
    private static int humongousMask;
    private static int startsHumongousTag;
    private static int continuesHumongousTag;
    private static int oldMask;
    private static CIntegerField tagField;
    private int tag;

    static {
        VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
        });
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("G1HeapRegionType");

        tagField = type.getCIntegerField("_tag");

        freeTag = db.lookupIntConstant("G1HeapRegionType::FreeTag");
        youngMask = db.lookupIntConstant("G1HeapRegionType::YoungMask");
        edenTag = db.lookupIntConstant("G1HeapRegionType::EdenTag");
        survTag = db.lookupIntConstant("G1HeapRegionType::SurvTag");
        startsHumongousTag = db.lookupIntConstant("G1HeapRegionType::StartsHumongousTag");
        continuesHumongousTag = db.lookupIntConstant("G1HeapRegionType::ContinuesHumongousTag");
        humongousMask = db.lookupIntConstant("G1HeapRegionType::HumongousMask");
        oldMask = db.lookupIntConstant("G1HeapRegionType::OldMask");
    }

    public boolean isFree() {
        return tagField.getValue(addr) == freeTag;
    }

    public boolean isEden() {
        return tagField.getValue(addr) == edenTag;
    }

    public boolean isSurvivor() {
        return tagField.getValue(addr) == survTag;
    }

    public boolean isYoung() {
        return (tagField.getValue(addr) & youngMask) != 0;
    }

    public boolean isHumongous() {
        return (tagField.getValue(addr) & humongousMask) != 0;
    }

    public boolean isStartsHumongous() {
        return tagField.getValue(addr) == startsHumongousTag;
    }

    public boolean isContinuesHumongous() {
        return tagField.getValue(addr) == continuesHumongousTag;
    }

    public boolean isOld() {
        return (tagField.getValue(addr) & oldMask) != 0;
    }

    public G1HeapRegionType(Address addr) {
        super(addr);
    }

    public String typeAnnotation() {
        if (isFree()) {
            return "Free";
        }
        if (isEden()) {
            return "Eden";
        }
        if (isSurvivor()) {
            return "Survivor";
        }
        if (isStartsHumongous()) {
            return "StartsHumongous";
        }
        if (isContinuesHumongous()) {
            return "ContinuesHumongous";
        }
        if (isOld()) {
            return "Old";
        }
        return "Unknown Region Type";
    }
}
