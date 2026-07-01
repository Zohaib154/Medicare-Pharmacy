using System;
using System.IO;
using System.IO.Compression;
using System.Drawing;
using System.Windows.Forms;
using System.Threading.Tasks;
using System.Diagnostics;

namespace MediCare.Installer
{
    public class Program
    {
        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new InstallerForm());
        }
    }

    public class InstallerForm : Form
    {
        private ProgressBar progressBar;
        private Label lblStatus;
        private Button btnCancel;

        public InstallerForm()
        {
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            this.progressBar = new ProgressBar();
            this.lblStatus = new Label();
            this.btnCancel = new Button();

            // Form
            this.Text = "MediCare Pharmacy System - Installation Wizard";
            this.Size = new Size(500, 220);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.BackColor = Color.FromArgb(15, 23, 42); // New premium slate dark theme
            this.ForeColor = Color.White;

            // Status Label
            this.lblStatus.Location = new Point(30, 30);
            this.lblStatus.Size = new Size(440, 25);
            this.lblStatus.Font = new Font("Segoe UI", 10F, FontStyle.Regular);
            this.lblStatus.Text = "Preparing to install MediCare Pharmacy Management System...";

            // Progress Bar
            this.progressBar.Location = new Point(30, 65);
            this.progressBar.Size = new Size(424, 25);
            this.progressBar.Style = ProgressBarStyle.Marquee;

            // Cancel Button
            this.btnCancel.Location = new Point(380, 120);
            this.btnCancel.Size = new Size(75, 30);
            this.btnCancel.Text = "Cancel";
            this.btnCancel.Font = new Font("Segoe UI", 9F);
            this.btnCancel.FlatStyle = FlatStyle.Flat;
            this.btnCancel.FlatAppearance.BorderColor = Color.FromArgb(100, 100, 100);
            this.btnCancel.Click += (s, e) => { Application.Exit(); };

            this.Controls.Add(this.lblStatus);
            this.Controls.Add(this.progressBar);
            this.Controls.Add(this.btnCancel);

            // Load and Run Installation
            this.Load += InstallerForm_Load;
        }

        private async void InstallerForm_Load(object sender, EventArgs e)
        {
            await Task.Delay(1000); // Small delay for user to see the start
            lblStatus.Text = "Extracting application components...";
            progressBar.Style = ProgressBarStyle.Continuous;
            progressBar.Value = 10;

            try
            {
                // Target directory: C:\Program Files\MediCare
                string programFiles = Environment.GetEnvironmentVariable("ProgramFiles");
                if (string.IsNullOrEmpty(programFiles))
                {
                    programFiles = @"C:\Program Files";
                }
                string installDir = Path.Combine(programFiles, "MediCare");

                if (Directory.Exists(installDir))
                {
                    try
                    {
                        Directory.Delete(installDir, true);
                    }
                    catch { /* Ignored if some file is locked temporarily */ }
                }

                Directory.CreateDirectory(installDir);
                progressBar.Value = 30;

                // Extract embedded app.zip resource
                await Task.Run(() =>
                {
                    var assembly = System.Reflection.Assembly.GetExecutingAssembly();
                    using (Stream stream = assembly.GetManifestResourceStream("app.zip"))
                    {
                        if (stream == null)
                        {
                            throw new Exception("Embedded resource 'app.zip' not found inside the installer package.");
                        }
                        using (ZipArchive archive = new ZipArchive(stream))
                        {
                            archive.ExtractToDirectory(installDir);
                        }
                    }
                });

                progressBar.Value = 70;
                lblStatus.Text = "Creating Desktop and Start Menu shortcuts...";

                // Create Shortcut Utility
                // Write to Common Desktop (All Users) so it appears on user's desktop
                string desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory);
                if (string.IsNullOrEmpty(desktopPath))
                {
                    desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                }
                string shortcutPath = Path.Combine(desktopPath, "MediCare.lnk");
                string targetExe = Path.Combine(installDir, "MediCare.exe");

                CreateShortcut(shortcutPath, targetExe, installDir, "MediCare Pharmacy Management System");

                // Start Menu shortcut (Common Start Menu for All Users)
                string startMenuPath = Environment.GetFolderPath(Environment.SpecialFolder.CommonStartMenu);
                if (string.IsNullOrEmpty(startMenuPath))
                {
                    startMenuPath = Environment.GetFolderPath(Environment.SpecialFolder.StartMenu);
                }
                string startMenuLnk = Path.Combine(startMenuPath, "Programs", "MediCare.lnk");
                CreateShortcut(startMenuLnk, targetExe, installDir, "MediCare Pharmacy Management System");

                progressBar.Value = 85;
                lblStatus.Text = "Registering application in Windows Registry...";

                // Register uninstaller under HKLM
                RegisterUninstaller(installDir, targetExe);

                progressBar.Value = 100;
                lblStatus.Text = "Installation completed successfully!";

                MessageBox.Show("MediCare Pharmacy Management System has been installed successfully!\n\nA shortcut has been created on your desktop.", "Installation Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);

                // Run the app and close installer
                Process.Start(new ProcessStartInfo
                {
                    FileName = targetExe,
                    WorkingDirectory = installDir
                });

                Application.Exit();
            }
            catch (Exception ex)
            {
                progressBar.Style = ProgressBarStyle.Continuous;
                progressBar.Value = 0;
                lblStatus.Text = "Error occurred during installation.";
                MessageBox.Show("Error: " + ex.Message, "Installation Failed", MessageBoxButtons.OK, MessageBoxIcon.Error);
                Application.Exit();
            }
        }

        private void CreateShortcut(string shortcutPath, string targetPath, string workingDir, string description)
        {
            try
            {
                Type shellType = Type.GetTypeFromProgID("WScript.Shell");
                dynamic shell = Activator.CreateInstance(shellType);
                dynamic shortcut = shell.CreateShortcut(shortcutPath);
                shortcut.TargetPath = targetPath;
                shortcut.WorkingDirectory = workingDir;
                shortcut.Description = description;
                // Set icon explicitly so shortcut shows the app icon, not a blank/black icon
                shortcut.IconLocation = targetPath + ",0";
                shortcut.Save();
            }
            catch (Exception ex)
            {
                Console.WriteLine("Shortcut creation error: " + ex.Message);
            }
        }


        private void RegisterUninstaller(string installDir, string targetExe)
        {
            try
            {
                string regPath = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\MediCare";
                using (var key = Microsoft.Win32.Registry.LocalMachine.CreateSubKey(regPath))
                {
                    if (key != null)
                    {
                        key.SetValue("DisplayName", "MediCare Pharmacy Management System");
                        key.SetValue("UninstallString", "\"" + Path.Combine(installDir, "uninstall.exe") + "\"");
                        key.SetValue("DisplayIcon", "\"" + targetExe + "\"");
                        key.SetValue("DisplayVersion", "1.0.0");
                        key.SetValue("Publisher", "MediCare");
                        key.SetValue("InstallLocation", installDir);
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Registry registration error: " + ex.Message);
            }
        }
    }
}
