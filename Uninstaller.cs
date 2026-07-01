using System;
using System.IO;
using System.Drawing;
using System.Windows.Forms;
using System.Diagnostics;
using System.Threading.Tasks;

namespace MediCare.Uninstaller
{
    public class Program
    {
        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new UninstallerForm());
        }
    }

    public class UninstallerForm : Form
    {
        private ProgressBar progressBar;
        private Label lblStatus;
        private Button btnCancel;

        public UninstallerForm()
        {
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            this.progressBar = new ProgressBar();
            this.lblStatus = new Label();
            this.btnCancel = new Button();

            // Form
            this.Text = "MediCare Pharmacy System - Uninstall";
            this.Size = new Size(500, 200);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.BackColor = Color.FromArgb(18, 22, 28);
            this.ForeColor = Color.White;

            // Status Label
            this.lblStatus.Location = new Point(30, 30);
            this.lblStatus.Size = new Size(440, 25);
            this.lblStatus.Font = new Font("Segoe UI", 10F, FontStyle.Regular);
            this.lblStatus.Text = "Preparing to uninstall MediCare Pharmacy Management System...";

            // Progress Bar
            this.progressBar.Location = new Point(30, 65);
            this.progressBar.Size = new Size(424, 25);
            this.progressBar.Style = ProgressBarStyle.Marquee;

            // Cancel Button
            this.btnCancel.Location = new Point(380, 110);
            this.btnCancel.Size = new Size(75, 30);
            this.btnCancel.Text = "Cancel";
            this.btnCancel.Font = new Font("Segoe UI", 9F);
            this.btnCancel.FlatStyle = FlatStyle.Flat;
            this.btnCancel.FlatAppearance.BorderColor = Color.FromArgb(100, 100, 100);
            this.btnCancel.Click += (s, e) => { Application.Exit(); };

            this.Controls.Add(this.lblStatus);
            this.Controls.Add(this.progressBar);
            this.Controls.Add(this.btnCancel);

            this.Load += UninstallerForm_Load;
        }

        private async void UninstallerForm_Load(object sender, EventArgs e)
        {
            var result = MessageBox.Show(
                "Are you sure you want to completely remove MediCare Pharmacy Management System and all of its components?",
                "Confirm Uninstall",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question
            );

            if (result != DialogResult.Yes)
            {
                Application.Exit();
                return;
            }

            try
            {
                lblStatus.Text = "Uninstalling components...";
                progressBar.Style = ProgressBarStyle.Continuous;
                progressBar.Value = 20;

                string programFiles = Environment.GetEnvironmentVariable("ProgramFiles");
                if (string.IsNullOrEmpty(programFiles))
                {
                    programFiles = @"C:\Program Files";
                }
                string installDir = Path.Combine(programFiles, "MediCare");

                // 1. Remove Registry Key under HKLM
                lblStatus.Text = "Removing registry settings...";
                try
                {
                    string regPath = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\MediCare";
                    Microsoft.Win32.Registry.LocalMachine.DeleteSubKey(regPath, false);
                }
                catch { }
                progressBar.Value = 50;

                // 2. Remove Shortcuts from Common Folders
                lblStatus.Text = "Removing shortcuts...";
                try
                {
                    string desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory);
                    if (string.IsNullOrEmpty(desktopPath))
                    {
                        desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                    }
                    string shortcutPath = Path.Combine(desktopPath, "MediCare.lnk");
                    if (File.Exists(shortcutPath)) File.Delete(shortcutPath);
                }
                catch { }

                try
                {
                    string startMenuPath = Environment.GetFolderPath(Environment.SpecialFolder.CommonStartMenu);
                    if (string.IsNullOrEmpty(startMenuPath))
                    {
                        startMenuPath = Environment.GetFolderPath(Environment.SpecialFolder.StartMenu);
                    }
                    string startMenuLnk = Path.Combine(startMenuPath, "Programs", "MediCare.lnk");
                    if (File.Exists(startMenuLnk)) File.Delete(startMenuLnk);
                }
                catch { }
                progressBar.Value = 80;

                // 3. Remove application directory self-clean trick
                lblStatus.Text = "Completing uninstallation...";
                progressBar.Value = 100;
                await Task.Delay(500);

                MessageBox.Show(
                    "MediCare Pharmacy Management System has been successfully uninstalled.",
                    "Uninstall Complete",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information
                );

                // Run self-delete cmd command after exit
                Process.Start(new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = "/c timeout /t 1 /nobreak && rmDir /s /q \"" + installDir + "\"",
                    CreateNoWindow = true,
                    WindowStyle = ProcessWindowStyle.Hidden
                });

                Application.Exit();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Uninstall error: " + ex.Message, "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                Application.Exit();
            }
        }
    }
}
