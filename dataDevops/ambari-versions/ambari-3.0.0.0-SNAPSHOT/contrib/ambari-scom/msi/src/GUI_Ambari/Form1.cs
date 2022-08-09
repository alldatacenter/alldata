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

using System;
using System.IO;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Text.RegularExpressions;
using System.Diagnostics;

namespace GUI_Ambari
{
    public partial class Form1 : Form
    {
        public Form1(string upgrade)
        {

            InitializeComponent();
            if (!string.IsNullOrEmpty(upgrade))
            {
                DBdel.Checked = false;
                Install.Text = "Upgrade";
            }

        }

        private void Form1_Load(object sender, EventArgs e)
        {
            Sname.Text = Environment.GetEnvironmentVariable("computername");
            CLP_Path();
        }
        private void CLP_Path()
        { 
             if (!string.IsNullOrEmpty (Environment.GetEnvironmentVariable("HADOOP_HOME")))
            {
                try
                {
                    Cpath.Text = Path.GetFullPath(Environment.GetEnvironmentVariable("HADOOP_NODE_INSTALL_ROOT") + @"\cluster.properties");
                }
                catch 
                { }
            }
        }
        private void Browse_Click(object sender, EventArgs e)
        {
            BrowseDirs.ShowDialog();

            if (BrowseDirs.SelectedPath.ToString().Contains(" "))
            {
                MessageBox.Show("Please select correct log directory. Directories containing spaces are disallowed", "Error");


            }
            else if (BrowseDirs.SelectedPath.ToString().Length == 3)
            {
                MessageBox.Show("Please select correct log directory. Root directories are disallowed", "Error");

            }
            else
            {

                AID.Text = BrowseDirs.SelectedPath;
            }
        }

        private void Sport_TextChanged(object sender, EventArgs e)
        {
            string test = Sport.Text.ToString();


            if (!Regex.IsMatch(test, "[0-9]") || (Convert.ToInt32(test) > 65536))
            {
                Sport.Clear();
            }

        }

        private void Install_Click(object sender, EventArgs e) 
        {
            Regex rgx = new Regex(@"(\d+\.\d+\.\d+)");
            if (string.IsNullOrEmpty(AID.Text) || string.IsNullOrEmpty(Sname.Text) || string.IsNullOrEmpty(Sport.Text) || string.IsNullOrEmpty(Slogin.Text) || string.IsNullOrEmpty(Spassword.Text) || string.IsNullOrEmpty(SQLDpath.Text))
            {
                MessageBox.Show("Please fill in all fields", "Error");
            }
            else if (string.IsNullOrEmpty(AID.Text) || AID.Text.Contains(" ") || !AID.Text.Contains("\\") || !AID.Text.Contains(":") || (AID.Text.Length < 4))
            {
                MessageBox.Show("Please enter correct Ambari directory", "Error");
            }
            else if (string.IsNullOrEmpty(SQLDpath.Text) || SQLDpath.Text.Contains(" ") || !SQLDpath.Text.Contains("\\") || !SQLDpath.Text.Contains(":") || (SQLDpath.Text.Length < 4)|| !SQLDpath.Text.Contains(".jar"))
            {
                MessageBox.Show("Please enter correct SQL JDBC driver path", "Error");
            }
            else if (string.IsNullOrEmpty(Cpath.Text) & string.IsNullOrEmpty (Environment.GetEnvironmentVariable("HADOOP_HOME")))
            {
                MessageBox.Show("You are installing Ambari on separate node. Please enter correct cluster layout file path", "Error");
            }
            else if (Userdetect.Checked && !rgx.IsMatch(MainVersion.Text))
            {
                MessageBox.Show("Please provide HDP version in correct format. For example 2.0.6", "Error");
            }
            else
            {
                //if (!(Sname.Text == Environment.GetEnvironmentVariable("computername")))
                //{
                //    Validate_Hosts();
                //}
                //else
                //{
                Generate_Ambari_Props();
                //}

            }
        }

        private void Cancel_Click(object sender, EventArgs e)
        {
            DialogResult result = MessageBox.Show("Do you really want to exit?", "Warning", MessageBoxButtons.YesNo);
            if (result == DialogResult.Yes)
            {
                GUI_Ambari.Program.Kill_Msiexec();
                Environment.Exit(1);
            }
        }

        private void Reset_Click(object sender, EventArgs e)
        {
            AID.Text = "C:\\Ambari";
            Sname.Text = Environment.GetEnvironmentVariable("computername");
            Sport.Text = "1433";
            Slogin.Clear();
            Spassword.Clear();
            Spassworde.Checked = false;
            Cpath.Clear();
            SQLDpath.Clear();
            Cstart.Checked = false;
            DBdel.Checked = true;
            Autodetect.Checked = true;
            CLP_Path();
        }

        private void Spassworde_CheckedChanged(object sender, EventArgs e)
        {
            if (Spassworde.Checked)
            {
                Spassword.UseSystemPasswordChar = false;
                Spassword.Text = Spassword.Text;
            }
            else
            {
                Spassword.UseSystemPasswordChar = true;
                Spassword.Text = Spassword.Text;
            }
        }
        private void Generate_Ambari_Props()
        {
            string res = Environment.GetEnvironmentVariable("appdata") + "\\amb_install";
            if (System.IO.Directory.Exists(res))
            {
                System.IO.Directory.Delete(res, true);
                System.IO.Directory.CreateDirectory(res);
            }
            if (!System.IO.Directory.Exists(res))
            {
                System.IO.Directory.CreateDirectory(res);
            }
            string cp = res + "\\ambariproperties.txt";
            using (StreamWriter sw = File.CreateText(cp))
            {
                sw.WriteLine("AMB_DATA_DIR=" + AID.Text);
                sw.WriteLine("SQL_SERVER_NAME=" + Sname.Text);
                sw.WriteLine("SQL_SERVER_LOGIN=" + Slogin.Text);
                sw.WriteLine("SQL_SERVER_PASSWORD=" + Spassword.Text);
                sw.WriteLine("SQL_SERVER_PORT=" + Sport.Text);
                sw.WriteLine("SQL_JDBC_PATH=" + SQLDpath.Text);
                if (Cstart.Checked == true)
                {
                    Environment.SetEnvironmentVariable("START_SERVICES", "yes", EnvironmentVariableTarget.Machine);
                }
                else
                {
                    Environment.SetEnvironmentVariable("START_SERVICES", "no", EnvironmentVariableTarget.Machine);
                }
                if (DBdel.Checked)
                {
                    Environment.SetEnvironmentVariable("RECREATE_DB", "yes", EnvironmentVariableTarget.Machine);
                }
                else
                {
                    Environment.SetEnvironmentVariable("RECREATE_DB", "no", EnvironmentVariableTarget.Machine);
                }
                if (Userdetect.Checked == true)
                {
                    sw.WriteLine("HDP_VERSION=" + MainVersion.Text);
                }
             }
            Environment.SetEnvironmentVariable("HDP_LAYOUT", Cpath.Text, EnvironmentVariableTarget.Machine);
            Environment.Exit(0);
        }
        private void Validate_Hosts()
        {

            foreach (Control c in this.Controls)
            {
                c.Enabled = false;
            }
            string failed = "";

            failed = ping(Sname.Text, failed);



            if (!string.IsNullOrEmpty(failed))
            {
                DialogResult result = MessageBox.Show(new Form() { TopMost = true }, "SQL Server host is not accessible:\r\n" + failed + "Do you want to continue installation with inaccessible SQL Server host?", "Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (result == DialogResult.Yes)
                {
                    Generate_Ambari_Props();
                }
                else
                {
                    foreach (Control c in this.Controls)
                    {
                        c.Enabled = true;
                    }
                }
            }
            
        }
        private string ping(string host, string failed)
        {

            Process process = new Process();
            process.StartInfo.FileName = "C:\\windows\\system32\\ping.exe";
            process.StartInfo.Arguments = host;
            process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            process.Start();
            process.WaitForExit();
            int code = process.ExitCode;
            if (code == 1)
            {
                failed = failed + host + "\r\n";
            }

            return failed;
        }

        private void Cbrowse_Click(object sender, EventArgs e)
        {
           OpenFileDialog OpenFile = new OpenFileDialog();
           OpenFile.Filter = "txt files (*.txt)|*.txt|All files (*.*)|*.*";
           OpenFile.InitialDirectory = @"C:\";
           OpenFile.Title = "Please select Ambari properties file";
           OpenFile.ShowDialog();
          
           if (OpenFile.FileName.ToString().Contains(" "))
            {
                MessageBox.Show("Please select correct path. Path containing spaces are disallowed", "Error");


            }
           else if (OpenFile.FileName.ToString().Length <= 4)
            {
                MessageBox.Show("Please select correct path", "Error");

            }
            else
            {

                Cpath.Text = OpenFile.FileName.ToString();
            }
        }

        private void SQLDbrowse_Click(object sender, EventArgs e)
        {
            OpenFileDialog OpenFile = new OpenFileDialog();
            OpenFile.Filter = "jar files (*.jar)|*.jar|All files (*.*)|*.*";
            OpenFile.InitialDirectory = @"C:\";
            OpenFile.Title = "Please select SQL JDBC driver.";
            OpenFile.ShowDialog();


            if (OpenFile.FileName.ToString().Contains(" "))
            {
                MessageBox.Show("Please select correct path. Path containing spaces are disallowed", "Error");


            }
            else if (OpenFile.FileName.ToString().Length <= 4)
            {
                MessageBox.Show("Please select correct path", "Error");

            }
            else
            {
                SQLDpath.Text = OpenFile.FileName.ToString();
            }
        }

        private void Userdetect_CheckedChanged(object sender, EventArgs e)
        {
            if (Userdetect.Checked)
            {
                MainVersion.Visible = true;
            }
        }

        private void Autodetect_CheckedChanged(object sender, EventArgs e)
        {
            if (Autodetect.Checked)
            {
                MainVersion.Visible = false;
                MainVersion.Clear();
            }
        }

    }
}
