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
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Diagnostics;

namespace GUI_Ambari
{
    static class Program
    {
      
        [STAThread]
        static void Main(string[] args)
        {
            string mode = args[0];
            string upgrade = null;
            try
            {
                upgrade = args[1];
            }
            catch { }
            switch (mode)
            {
                case "upgrade":
                    if (!String.IsNullOrEmpty(upgrade))
                    {
                        DialogResult upgrade_dial = MessageBox.Show("Old version of Ambari-SCOM detected. Do you want to perform upgrade?", "Ambari-SCOM Warning", MessageBoxButtons.YesNo, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button1, MessageBoxOptions.DefaultDesktopOnly);
                        if (upgrade_dial == DialogResult.No)
                        {
                            Kill_Msiexec();
                            Environment.Exit(0);
                        }
                    }

                    break;
                case "gui":
                    Application.EnableVisualStyles();
                    Application.SetCompatibleTextRenderingDefault(false);
                    Application.Run(new Form1(upgrade));
                break;
                case "install":
                string file_inst = Environment.GetEnvironmentVariable("tmp") + @"\ambari_failed.txt";
                    if (File.Exists(file_inst))
                    {
                        string text = File.ReadAllText(file_inst);
                        DialogResult result_inst = MessageBox.Show("Installation on next nodes failed.\r\nPlease proceed with manual installation for these nodes\r\n" + text, "Ambari-SCOM Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button1, MessageBoxOptions.DefaultDesktopOnly);
                        if (result_inst == DialogResult.OK)
                        {
                            File.Delete(file_inst);
                            Environment.Exit(0);
                        }
                    }
                  
                break;
                case "uninstall":
                    string file_un = Environment.GetEnvironmentVariable("tmp") + @"\ambari_failed.txt";
                    if (File.Exists(file_un))
                    {
                        string text = File.ReadAllText(file_un);
                        DialogResult result_un = MessageBox.Show("Uninstallation on next nodes failed.\r\nPlease proceed with manual uninstallation for these nodes\r\n" + text, "Ambari-SCOM Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button1,MessageBoxOptions.DefaultDesktopOnly);
                        if (result_un == DialogResult.OK)
                        {
                            File.Delete(file_un);
                            Environment.Exit(0);
                        }
                    }
                break;
                case "db":
                    string file_db = Environment.GetEnvironmentVariable("tmp") + @"\db_ambari_failed.txt";
                    if (File.Exists(file_db))
                    {
                        string text = File.ReadAllText(file_db);
                        DialogResult result_db = MessageBox.Show("Database creation on SQL server: " + text+" failed. Please create database manually", "Ambari-SCOM Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning, MessageBoxDefaultButton.Button1, MessageBoxOptions.DefaultDesktopOnly);
                        if (result_db == DialogResult.OK)
                        {
                            File.Delete(file_db);
                            Environment.Exit(0);
                        }
                    }
                break;

            }
        }

        public static void Kill_Msiexec()
        {
            try
            {
                var processes = Process.GetProcessesByName("msiexec").OrderBy(x => x.StartTime);
                foreach (var process in processes)
                {
                    process.Kill();
                }
            }
            catch
            {
            }
        }
    }
}
