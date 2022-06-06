// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate
{
    partial class WatcherNodesPage {
        /// <summary> 
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary> 
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing) {
            if (disposing && (components != null)) {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Component Designer generated code

        /// <summary> 
        /// Required method for Designer support - do not modify 
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent() {
            this.lblDescription = new System.Windows.Forms.Label();
            this.lblTitle = new Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel();
            this.listWatcherNodes = new System.Windows.Forms.ListBox();
            this.lblWatcherNodes = new System.Windows.Forms.Label();
            this.tsMenu = new System.Windows.Forms.ToolStrip();
            this.btnAdd = new System.Windows.Forms.ToolStripButton();
            this.separatorEdit = new System.Windows.Forms.ToolStripSeparator();
            this.btnRemove = new System.Windows.Forms.ToolStripButton();
            this.tsMenu.SuspendLayout();
            this.SuspendLayout();
            // 
            // lblDescription
            // 
            this.lblDescription.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.lblDescription.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblDescription.Location = new System.Drawing.Point(0, 43);
            this.lblDescription.Name = "lblDescription";
            this.lblDescription.Size = new System.Drawing.Size(438, 91);
            this.lblDescription.TabIndex = 10;
            this.lblDescription.Text = "Select an agent-managed computer to act as a watcher node. The watcher node commu" +
    "nicates with Ambari API to retrieve data.";
            // 
            // lblTitle
            // 
            this.lblTitle.BackColor = System.Drawing.Color.Transparent;
            this.lblTitle.Font = new System.Drawing.Font("Tahoma", 8.25F, System.Drawing.FontStyle.Bold);
            this.lblTitle.ImeMode = System.Windows.Forms.ImeMode.NoControl;
            this.lblTitle.Location = new System.Drawing.Point(0, 10);
            this.lblTitle.MinimumSize = new System.Drawing.Size(244, 0);
            this.lblTitle.Name = "lblTitle";
            this.lblTitle.Size = new System.Drawing.Size(244, 18);
            this.lblTitle.TabIndex = 9;
            this.lblTitle.Text = "Choose Watcher Nodes";
            // 
            // listWatcherNodes
            // 
            this.listWatcherNodes.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.listWatcherNodes.FormattingEnabled = true;
            this.listWatcherNodes.Location = new System.Drawing.Point(3, 177);
            this.listWatcherNodes.Name = "listWatcherNodes";
            this.listWatcherNodes.SelectionMode = System.Windows.Forms.SelectionMode.MultiExtended;
            this.listWatcherNodes.Size = new System.Drawing.Size(435, 108);
            this.listWatcherNodes.TabIndex = 11;
            this.listWatcherNodes.SelectedIndexChanged += new System.EventHandler(this.listWatcherNodes_SelectedIndexChanged);
            // 
            // lblWatcherNodes
            // 
            this.lblWatcherNodes.AutoSize = true;
            this.lblWatcherNodes.Location = new System.Drawing.Point(3, 153);
            this.lblWatcherNodes.Name = "lblWatcherNodes";
            this.lblWatcherNodes.Size = new System.Drawing.Size(85, 13);
            this.lblWatcherNodes.TabIndex = 14;
            this.lblWatcherNodes.Text = "Watcher Nodes:";
            // 
            // tsMenu
            // 
            this.tsMenu.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Right)));
            this.tsMenu.Dock = System.Windows.Forms.DockStyle.None;
            this.tsMenu.GripStyle = System.Windows.Forms.ToolStripGripStyle.Hidden;
            this.tsMenu.Items.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.btnAdd,
            this.separatorEdit,
            this.btnRemove});
            this.tsMenu.Location = new System.Drawing.Point(286, 149);
            this.tsMenu.Name = "tsMenu";
            this.tsMenu.RenderMode = System.Windows.Forms.ToolStripRenderMode.System;
            this.tsMenu.Size = new System.Drawing.Size(152, 25);
            this.tsMenu.TabIndex = 24;
            this.tsMenu.Text = "stripButtons";
            // 
            // btnAdd
            // 
            this.btnAdd.Image = global::Ambari.SCOM.ScomPages.Properties.Resources.Add;
            this.btnAdd.ImageAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.btnAdd.ImageTransparentColor = System.Drawing.Color.Magenta;
            this.btnAdd.Name = "btnAdd";
            this.btnAdd.Size = new System.Drawing.Size(46, 22);
            this.btnAdd.Text = "Add";
            this.btnAdd.TextAlign = System.Drawing.ContentAlignment.MiddleLeft;
            this.btnAdd.Click += new System.EventHandler(this.btnAdd_Click);
            // 
            // separatorEdit
            // 
            this.separatorEdit.Name = "separatorEdit";
            this.separatorEdit.Size = new System.Drawing.Size(6, 25);
            // 
            // btnRemove
            // 
            this.btnRemove.Enabled = false;
            this.btnRemove.Image = global::Ambari.SCOM.ScomPages.Properties.Resources.Remove;
            this.btnRemove.ImageTransparentColor = System.Drawing.Color.Magenta;
            this.btnRemove.Name = "btnRemove";
            this.btnRemove.Size = new System.Drawing.Size(66, 22);
            this.btnRemove.Text = "Remove";
            this.btnRemove.Click += new System.EventHandler(this.btnRemove_Click);
            // 
            // WatcherNodesPage
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this.tsMenu);
            this.Controls.Add(this.lblWatcherNodes);
            this.Controls.Add(this.listWatcherNodes);
            this.Controls.Add(this.lblDescription);
            this.Controls.Add(this.lblTitle);
            this.HeaderText = "Watcher Nodes";
            this.Name = "WatcherNodesPage";
            this.NavigationText = "Watcher Nodes";
            this.Size = new System.Drawing.Size(456, 391);
            this.TabName = "Watcher Nodes";
            this.tsMenu.ResumeLayout(false);
            this.tsMenu.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label lblDescription;
        private Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel lblTitle;
        private System.Windows.Forms.ListBox listWatcherNodes;
        private System.Windows.Forms.Label lblWatcherNodes;
        private System.Windows.Forms.ToolStrip tsMenu;
        private System.Windows.Forms.ToolStripButton btnAdd;
        private System.Windows.Forms.ToolStripSeparator separatorEdit;
        private System.Windows.Forms.ToolStripButton btnRemove;
    }
}
