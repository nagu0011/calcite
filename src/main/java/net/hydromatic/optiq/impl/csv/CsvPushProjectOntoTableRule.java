/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.csv;

import org.eigenbase.rel.ProjectRel;
import org.eigenbase.relopt.RelOptRule;
import org.eigenbase.relopt.RelOptRuleCall;
import org.eigenbase.relopt.RelOptRuleOperand;
import org.eigenbase.rex.RexInputRef;
import org.eigenbase.rex.RexNode;

/**
 * Planner rule that projects from a {@link CsvTableScan} scan just the columns
 * needed to satisfy a projection. If the projection's expressions are trivial,
 * the projection is removed.
 */
public class CsvPushProjectOntoTableRule extends RelOptRule {
  public static final CsvPushProjectOntoTableRule INSTANCE =
      new CsvPushProjectOntoTableRule();

  private CsvPushProjectOntoTableRule() {
    super(
        new RelOptRuleOperand(
            ProjectRel.class,
            new RelOptRuleOperand(
                CsvTableScan.class)),
        "CsvPushProjectOntoTableRule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final ProjectRel project = (ProjectRel) call.getRels()[0];
    final CsvTableScan scan = (CsvTableScan) call.getRels()[1];
    int[] fields = getProjectFields(project.getProjectExps());
    if (fields == null) {
      // Project contains expressions more complex than just field references.
      return;
    }
    call.transformTo(
        new CsvTableScan(
            scan.getCluster(),
            scan.getTable(),
            scan.csvTable,
            fields));
  }

  private int[] getProjectFields(RexNode[] exps) {
    final int[] fields = new int[exps.length];
    for (int i = 0; i < exps.length; i++) {
      final RexNode exp = exps[i];
      if (exp instanceof RexInputRef) {
        fields[i] = ((RexInputRef) exp).getIndex();
      } else {
        return null; // not a simple projection
      }
    }
    return fields;
  }
}

// End CsvPushProjectOntoTableRule.java
