/*
 * Copyright (c) 2002, 2025, Oracle and/or its affiliates. All rights reserved.
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
 */

package nsk.jdi.ThreadReference.popFrames;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a debuggee class.
 */
public class popframes006t {
    static Thread testThread = null;

    private Log log;
    private IOPipe pipe;
    volatile boolean wasPopped = false;

    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new popframes006t().runIt(args);
    }

    private int runIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);

        log = argHandler.createDebugeeLog();
        pipe = argHandler.createDebugeeIOPipe();

        testThread = Thread.currentThread();
        testThread.setName(popframes006.DEBUGGEE_THRNAME);

        pipe.println(popframes006.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(popframes006.COMMAND_QUIT)) {
            log.complain("Debuggee: exiting due to the command "
                    + cmd);
            return Consts.TEST_PASSED;
        }

        frameToBePopped();

        cmd = pipe.readln();
        if (!cmd.equals(popframes006.COMMAND_QUIT)) {
            log.complain("TEST BUG: unknown debugger command: "
                + cmd);
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }

    private void frameToBePopped() {
        popframes006t popframes006tFindMe = this;

        log.display("frameToBePopped: enter frameToBePopped()");

        int stopMeHere = 0; // popframes006.DEBUGGEE_STOPATLINE

        while(!wasPopped) { // looping
            stopMeHere++; stopMeHere--;
        }

        log.display("frameToBePopped: leaving frameToBePopped()");
    }
}
