//Licensed to the Apache Software Foundation (ASF) under one or more
//contributor license agreements.  See the NOTICE file distributed with
//this work for additional information regarding copyright ownership.
//The ASF licenses this file to You under the Apache License, Version 2.0
//(the "License"); you may not use this file except in compliance with
//the License.  You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
namespace GUI_Ambari
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.Install = new System.Windows.Forms.Button();
            this.AID = new System.Windows.Forms.TextBox();
            this.Browse = new System.Windows.Forms.Button();
            this.Sname = new System.Windows.Forms.TextBox();
            this.label1 = new System.Windows.Forms.Label();
            this.asda = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            this.Sport = new System.Windows.Forms.TextBox();
            this.label3 = new System.Windows.Forms.Label();
            this.Slogin = new System.Windows.Forms.TextBox();
            this.label4 = new System.Windows.Forms.Label();
            this.Spassword = new System.Windows.Forms.TextBox();
            this.Cancel = new System.Windows.Forms.Button();
            this.Reset = new System.Windows.Forms.Button();
            this.BrowseDirs = new System.Windows.Forms.FolderBrowserDialog();
            this.Spassworde = new System.Windows.Forms.CheckBox();
            this.label5 = new System.Windows.Forms.Label();
            this.Cbrowse = new System.Windows.Forms.Button();
            this.Cpath = new System.Windows.Forms.TextBox();
            this.OpenFile = new System.Windows.Forms.OpenFileDialog();
            this.label6 = new System.Windows.Forms.Label();
            this.SQLDbrowse = new System.Windows.Forms.Button();
            this.SQLDpath = new System.Windows.Forms.TextBox();
            this.Cstart = new System.Windows.Forms.CheckBox();
            this.DBdel = new System.Windows.Forms.CheckBox();
            this.groupBox1 = new System.Windows.Forms.GroupBox();
            this.Userdetect = new System.Windows.Forms.RadioButton();
            this.Autodetect = new System.Windows.Forms.RadioButton();
            this.MainVersion = new System.Windows.Forms.TextBox();
            this.groupBox1.SuspendLayout();
            this.SuspendLayout();
            // 
            // Install
            // 
            this.Install.Location = new System.Drawing.Point(136, 348);
            this.Install.Name = "Install";
            this.Install.Size = new System.Drawing.Size(75, 23);
            this.Install.TabIndex = 0;
            this.Install.Text = "Install";
            this.Install.UseVisualStyleBackColor = true;
            this.Install.Click += new System.EventHandler(this.Install_Click);
            // 
            // AID
            // 
            this.AID.Location = new System.Drawing.Point(12, 24);
            this.AID.Name = "AID";
            this.AID.Size = new System.Drawing.Size(280, 20);
            this.AID.TabIndex = 1;
            this.AID.Text = "C:\\Ambari";
            // 
            // Browse
            // 
            this.Browse.Location = new System.Drawing.Point(298, 21);
            this.Browse.Name = "Browse";
            this.Browse.Size = new System.Drawing.Size(75, 23);
            this.Browse.TabIndex = 2;
            this.Browse.Text = "Browse";
            this.Browse.UseVisualStyleBackColor = true;
            this.Browse.Click += new System.EventHandler(this.Browse_Click);
            // 
            // Sname
            // 
            this.Sname.Location = new System.Drawing.Point(12, 63);
            this.Sname.Name = "Sname";
            this.Sname.Size = new System.Drawing.Size(280, 20);
            this.Sname.TabIndex = 3;
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(12, 9);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(161, 13);
            this.label1.TabIndex = 4;
            this.label1.Text = "Ambari SCOM package directory";
            // 
            // asda
            // 
            this.asda.AutoSize = true;
            this.asda.Location = new System.Drawing.Point(12, 47);
            this.asda.Name = "asda";
            this.asda.Size = new System.Drawing.Size(111, 13);
            this.asda.TabIndex = 5;
            this.asda.Text = "SQL Server hostname";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(12, 86);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(83, 13);
            this.label2.TabIndex = 7;
            this.label2.Text = "SQL Server port";
            // 
            // Sport
            // 
            this.Sport.Location = new System.Drawing.Point(12, 102);
            this.Sport.Name = "Sport";
            this.Sport.Size = new System.Drawing.Size(280, 20);
            this.Sport.TabIndex = 6;
            this.Sport.Text = "1433";
            this.Sport.TextChanged += new System.EventHandler(this.Sport_TextChanged);
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Location = new System.Drawing.Point(12, 125);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(87, 13);
            this.label3.TabIndex = 9;
            this.label3.Text = "SQL Server login";
            // 
            // Slogin
            // 
            this.Slogin.Location = new System.Drawing.Point(12, 141);
            this.Slogin.Name = "Slogin";
            this.Slogin.Size = new System.Drawing.Size(280, 20);
            this.Slogin.TabIndex = 8;
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Location = new System.Drawing.Point(12, 164);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(110, 13);
            this.label4.TabIndex = 11;
            this.label4.Text = "SQL Server password";
            // 
            // Spassword
            // 
            this.Spassword.Location = new System.Drawing.Point(12, 180);
            this.Spassword.Name = "Spassword";
            this.Spassword.Size = new System.Drawing.Size(277, 20);
            this.Spassword.TabIndex = 10;
            this.Spassword.UseSystemPasswordChar = true;
            // 
            // Cancel
            // 
            this.Cancel.DialogResult = System.Windows.Forms.DialogResult.Cancel;
            this.Cancel.Location = new System.Drawing.Point(217, 348);
            this.Cancel.Name = "Cancel";
            this.Cancel.Size = new System.Drawing.Size(75, 23);
            this.Cancel.TabIndex = 12;
            this.Cancel.Text = "Cancel";
            this.Cancel.UseVisualStyleBackColor = true;
            this.Cancel.Click += new System.EventHandler(this.Cancel_Click);
            // 
            // Reset
            // 
            this.Reset.Location = new System.Drawing.Point(298, 348);
            this.Reset.Name = "Reset";
            this.Reset.Size = new System.Drawing.Size(75, 23);
            this.Reset.TabIndex = 13;
            this.Reset.Text = "Reset";
            this.Reset.UseVisualStyleBackColor = true;
            this.Reset.Click += new System.EventHandler(this.Reset_Click);
            // 
            // BrowseDirs
            // 
            this.BrowseDirs.RootFolder = System.Environment.SpecialFolder.MyComputer;
            // 
            // Spassworde
            // 
            this.Spassworde.AutoSize = true;
            this.Spassworde.Location = new System.Drawing.Point(295, 182);
            this.Spassworde.Name = "Spassworde";
            this.Spassworde.Size = new System.Drawing.Size(78, 17);
            this.Spassworde.TabIndex = 14;
            this.Spassworde.Text = "Show pass";
            this.Spassworde.UseVisualStyleBackColor = true;
            this.Spassworde.CheckedChanged += new System.EventHandler(this.Spassworde_CheckedChanged);
            // 
            // label5
            // 
            this.label5.AutoSize = true;
            this.label5.Location = new System.Drawing.Point(12, 241);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(219, 13);
            this.label5.TabIndex = 17;
            this.label5.Text = "Path to cluster layout file (clusterpoperties.txt)";
            // 
            // Cbrowse
            // 
            this.Cbrowse.Location = new System.Drawing.Point(298, 253);
            this.Cbrowse.Name = "Cbrowse";
            this.Cbrowse.Size = new System.Drawing.Size(75, 23);
            this.Cbrowse.TabIndex = 16;
            this.Cbrowse.Text = "Browse";
            this.Cbrowse.UseVisualStyleBackColor = true;
            this.Cbrowse.Click += new System.EventHandler(this.Cbrowse_Click);
            // 
            // Cpath
            // 
            this.Cpath.Location = new System.Drawing.Point(12, 256);
            this.Cpath.Name = "Cpath";
            this.Cpath.Size = new System.Drawing.Size(277, 20);
            this.Cpath.TabIndex = 15;
            // 
            // OpenFile
            // 
            this.OpenFile.DefaultExt = "*.txt";
            // 
            // label6
            // 
            this.label6.AutoSize = true;
            this.label6.Location = new System.Drawing.Point(12, 203);
            this.label6.Name = "label6";
            this.label6.Size = new System.Drawing.Size(222, 13);
            this.label6.TabIndex = 20;
            this.label6.Text = "Path to SQL Server JDBC Driver (sqljdbc4.jar)";
            // 
            // SQLDbrowse
            // 
            this.SQLDbrowse.Location = new System.Drawing.Point(298, 215);
            this.SQLDbrowse.Name = "SQLDbrowse";
            this.SQLDbrowse.Size = new System.Drawing.Size(75, 23);
            this.SQLDbrowse.TabIndex = 19;
            this.SQLDbrowse.Text = "Browse";
            this.SQLDbrowse.UseVisualStyleBackColor = true;
            this.SQLDbrowse.Click += new System.EventHandler(this.SQLDbrowse_Click);
            // 
            // SQLDpath
            // 
            this.SQLDpath.Location = new System.Drawing.Point(12, 218);
            this.SQLDpath.Name = "SQLDpath";
            this.SQLDpath.Size = new System.Drawing.Size(277, 20);
            this.SQLDpath.TabIndex = 18;
            // 
            // Cstart
            // 
            this.Cstart.AutoSize = true;
            this.Cstart.Location = new System.Drawing.Point(12, 349);
            this.Cstart.Name = "Cstart";
            this.Cstart.Size = new System.Drawing.Size(92, 17);
            this.Cstart.TabIndex = 21;
            this.Cstart.Text = "Start Services";
            this.Cstart.UseVisualStyleBackColor = true;
            // 
            // DBdel
            // 
            this.DBdel.AutoSize = true;
            this.DBdel.Checked = true;
            this.DBdel.CheckState = System.Windows.Forms.CheckState.Checked;
            this.DBdel.Location = new System.Drawing.Point(295, 65);
            this.DBdel.Name = "DBdel";
            this.DBdel.Size = new System.Drawing.Size(88, 17);
            this.DBdel.TabIndex = 22;
            this.DBdel.Text = "Recreate DB";
            this.DBdel.UseVisualStyleBackColor = true;
            // 
            // groupBox1
            // 
            this.groupBox1.Controls.Add(this.Userdetect);
            this.groupBox1.Controls.Add(this.Autodetect);
            this.groupBox1.Controls.Add(this.MainVersion);
            this.groupBox1.Location = new System.Drawing.Point(15, 282);
            this.groupBox1.Name = "groupBox1";
            this.groupBox1.Size = new System.Drawing.Size(204, 61);
            this.groupBox1.TabIndex = 23;
            this.groupBox1.TabStop = false;
            // 
            // Userdetect
            // 
            this.Userdetect.AutoSize = true;
            this.Userdetect.Location = new System.Drawing.Point(7, 34);
            this.Userdetect.Name = "Userdetect";
            this.Userdetect.Size = new System.Drawing.Size(123, 17);
            this.Userdetect.TabIndex = 1;
            this.Userdetect.TabStop = true;
            this.Userdetect.Text = "Specify HDP version";
            this.Userdetect.UseVisualStyleBackColor = true;
            this.Userdetect.CheckedChanged += new System.EventHandler(this.Userdetect_CheckedChanged);
            // 
            // Autodetect
            // 
            this.Autodetect.AutoSize = true;
            this.Autodetect.Checked = true;
            this.Autodetect.Location = new System.Drawing.Point(7, 10);
            this.Autodetect.Name = "Autodetect";
            this.Autodetect.Size = new System.Drawing.Size(184, 17);
            this.Autodetect.TabIndex = 0;
            this.Autodetect.TabStop = true;
            this.Autodetect.Text = "Detect HDP version automatically";
            this.Autodetect.UseVisualStyleBackColor = true;
            this.Autodetect.CheckedChanged += new System.EventHandler(this.Autodetect_CheckedChanged);
            // 
            // MainVersion
            // 
            this.MainVersion.Location = new System.Drawing.Point(134, 34);
            this.MainVersion.MaxLength = 5;
            this.MainVersion.Name = "MainVersion";
            this.MainVersion.Size = new System.Drawing.Size(57, 20);
            this.MainVersion.TabIndex = 24;
            this.MainVersion.Visible = false;
            // 
            // Form1
            // 
            this.AcceptButton = this.Install;
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.CancelButton = this.Cancel;
            this.ClientSize = new System.Drawing.Size(381, 378);
            this.ControlBox = false;
            this.Controls.Add(this.groupBox1);
            this.Controls.Add(this.DBdel);
            this.Controls.Add(this.Cstart);
            this.Controls.Add(this.label6);
            this.Controls.Add(this.SQLDbrowse);
            this.Controls.Add(this.SQLDpath);
            this.Controls.Add(this.label5);
            this.Controls.Add(this.Cbrowse);
            this.Controls.Add(this.Cpath);
            this.Controls.Add(this.Spassworde);
            this.Controls.Add(this.Reset);
            this.Controls.Add(this.Cancel);
            this.Controls.Add(this.label4);
            this.Controls.Add(this.Spassword);
            this.Controls.Add(this.label3);
            this.Controls.Add(this.Slogin);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.Sport);
            this.Controls.Add(this.asda);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.Sname);
            this.Controls.Add(this.Browse);
            this.Controls.Add(this.AID);
            this.Controls.Add(this.Install);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedToolWindow;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.Name = "Form1";
            this.ShowIcon = false;
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "Ambari-SCOM setup";
            this.TopMost = true;
            this.Load += new System.EventHandler(this.Form1_Load);
            this.groupBox1.ResumeLayout(false);
            this.groupBox1.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Button Install;
        private System.Windows.Forms.TextBox AID;
        private System.Windows.Forms.Button Browse;
        private System.Windows.Forms.TextBox Sname;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Label asda;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.TextBox Sport;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.TextBox Slogin;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.TextBox Spassword;
        private System.Windows.Forms.Button Cancel;
        private System.Windows.Forms.Button Reset;
        private System.Windows.Forms.FolderBrowserDialog BrowseDirs;
        private System.Windows.Forms.CheckBox Spassworde;
        private System.Windows.Forms.Label label5;
        private System.Windows.Forms.Button Cbrowse;
        private System.Windows.Forms.TextBox Cpath;
        private System.Windows.Forms.OpenFileDialog OpenFile;
        private System.Windows.Forms.Label label6;
        private System.Windows.Forms.Button SQLDbrowse;
        private System.Windows.Forms.TextBox SQLDpath;
        private System.Windows.Forms.CheckBox Cstart;
        private System.Windows.Forms.CheckBox DBdel;
        private System.Windows.Forms.GroupBox groupBox1;
        private System.Windows.Forms.RadioButton Userdetect;
        private System.Windows.Forms.RadioButton Autodetect;
        private System.Windows.Forms.TextBox MainVersion;
       
    }
}

