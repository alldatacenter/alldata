/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.drill.exec.planner.physical.DirectScanPrel;
import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.JoinPrel;
import org.apache.drill.exec.planner.physical.LeafPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.ScreenPrel;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.planner.physical.UnnestPrel;
import org.apache.drill.exec.planner.physical.LateralJoinPrel;

/**
 * Debug-time class that prints a PRel tree to the console for
 * inspection. Insert this into code during development to see
 * the state of the tree at various points of interest during
 * the planning process.
 * <p>
 * Use this by inserting lines into our prel transforms to see
 * what is happening. This is useful if you must understand the transforms,
 * or change them. For example:
 * <p>
 * In file: {@link DefaultSqlHandler#convertToPrel()}:
 * <pre><code>
 * PrelVisualizerVisitor.print("Before EER", phyRelNode); // Debug only
 * phyRelNode = ExcessiveExchangeIdentifier.removeExcessiveEchanges(phyRelNode, targetSliceSize);
 * PrelVisualizerVisitor.print("After EER", phyRelNode); // Debug only
 * <code></pre>
 */

public class PrelVisualizerVisitor
    implements PrelVisitor<Void, PrelVisualizerVisitor.VisualizationState, Exception> {

  public static class VisualizationState {

    public static String INDENT = "  ";

    StringBuilder out = new StringBuilder();
    int level;

    public void startNode(Prel prel) {
      indent();
      out.append("{ ");
      out.append(prel.getClass().getSimpleName());
      out.append("\n");
      push();
    }

    public void endNode() {
      pop();
      indent();
      out.append("}");
      out.append("\n");
    }

    private void indent() {
      for (int i = 0; i < level; i++) {
        out.append(INDENT);
      }
    }

    public void push() {
      level++;
    }

    public void pop() {
      level--;
    }

    public void endFields() {
      // TODO Auto-generated method stub

    }

    public void visitField(String label, boolean value) {
      visitField(label, Boolean.toString(value));
    }

    private void visitField(String label, String value) {
      indent();
      out.append(label)
         .append(" = ")
         .append(value)
         .append("\n");
    }

    public void visitField(String label,
        Object[] values) {
      if (values == null) {
        visitField(label, "null");
        return;
      }
      StringBuilder buf = new StringBuilder();
      buf.append("[");
      boolean first = true;
      for (Object obj : values) {
        if (! first) {
          buf.append(", ");
        }
        first = false;
        if (obj == null) {
          buf.append("null");
        } else {
          buf.append(obj.toString());
        }
      }
      buf.append("]");
      visitField(label, buf.toString());
    }

    @Override
    public String toString() {
      return out.toString();
    }

  }

  public static void print(String label, Prel prel) {
    System.out.println(label);
    System.out.println(visualize(prel));
  }

  public static String visualize(Prel prel) {
    try {
      VisualizationState state = new VisualizationState();
      prel.accept(new PrelVisualizerVisitor(), state);
      return state.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return "** ERROR **";
    }
  }

  @Override
  public Void visitExchange(ExchangePrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  private void visitBasePrel(Prel prel, VisualizationState value) {
    value.startNode(prel);
    value.visitField("encodings", prel.getSupportedEncodings());
    value.visitField("needsReorder", prel.needsFinalColumnReordering());
  }

  private void endNode(Prel prel, VisualizationState value) throws Exception {
    value.endFields();
    visitChildren(prel, value);
    value.endNode();
  }

  private void visitChildren(Prel prel, VisualizationState value) throws Exception {
    value.indent();
    value.out.append("children = [\n");
    value.push();
    for (Prel child : prel) {
      child.accept(this, value);
    }
    value.pop();
    value.indent();
    value.out.append("]\n");
  }

  @Override
  public Void visitScreen(ScreenPrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitWriter(WriterPrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitScan(ScanPrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitScan(DirectScanPrel prel, VisualizationState value) throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitJoin(JoinPrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitProject(ProjectPrel prel, VisualizationState value)
      throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitPrel(Prel prel, VisualizationState value) throws Exception {
    visitBasePrel(prel, value);
    endNode(prel, value);
    return null;
  }

  @Override
  public Void visitUnnest(UnnestPrel prel, VisualizationState value) throws Exception {
    visitPrel(prel, value);
    return null;
  }

  @Override
  public Void visitLateral(LateralJoinPrel prel, VisualizationState value) throws Exception {
    visitPrel(prel, value);
    return null;
  }

  @Override
  public Void visitLeaf(LeafPrel prel, VisualizationState value) throws Exception {
    visitPrel(prel, value);
    return null;
  }
}
