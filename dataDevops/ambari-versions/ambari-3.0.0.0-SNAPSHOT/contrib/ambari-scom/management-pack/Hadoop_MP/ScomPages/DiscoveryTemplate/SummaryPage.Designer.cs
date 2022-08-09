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
    partial class SummaryPage {
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
            this.lblTitle = new Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel();
            this.listviewConfiguration = new System.Windows.Forms.ListView();
            this.columnHeader1 = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.columnHeader2 = ((System.Windows.Forms.ColumnHeader)(new System.Windows.Forms.ColumnHeader()));
            this.SuspendLayout();
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
            this.lblTitle.TabIndex = 3;
            this.lblTitle.Text = "Confirm the settings";
            // 
            // listviewConfiguration
            // 
            this.listviewConfiguration.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.listviewConfiguration.Columns.AddRange(new System.Windows.Forms.ColumnHeader[] {
            this.columnHeader1,
            this.columnHeader2});
            this.listviewConfiguration.Location = new System.Drawing.Point(0, 44);
            this.listviewConfiguration.Name = "listviewConfiguration";
            this.listviewConfiguration.Size = new System.Drawing.Size(435, 256);
            this.listviewConfiguration.TabIndex = 2;
            this.listviewConfiguration.UseCompatibleStateImageBehavior = false;
            this.listviewConfiguration.View = System.Windows.Forms.View.Details;
            // 
            // columnHeader1
            // 
            this.columnHeader1.Text = "Property";
            // 
            // columnHeader2
            // 
            this.columnHeader2.Text = "Value";
            // 
            // SummaryPage
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this.lblTitle);
            this.Controls.Add(this.listviewConfiguration);
            this.HeaderText = "Summary";
            this.Name = "SummaryPage";
            this.NavigationText = "Summary";
            this.Size = new System.Drawing.Size(456, 400);
            this.TabName = "Summary";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls.PageSectionLabel lblTitle;
        private System.Windows.Forms.ListView listviewConfiguration;
        private System.Windows.Forms.ColumnHeader columnHeader1;
        private System.Windows.Forms.ColumnHeader columnHeader2;
    }
}
