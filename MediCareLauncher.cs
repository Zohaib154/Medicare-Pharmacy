using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

namespace MediCare.Launcher
{
    public class Program
    {
        [STAThread]
        public static void Main()
        {
            try
            {
                // Get the executable directory
                string exeDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
                string projectRoot = exeDir;

                // Check if running from project root or from within target directory
                if (Directory.Exists(Path.Combine(projectRoot, "target")))
                {
                    // Running from project root
                    string jarPath = Path.Combine(projectRoot, "target", "medicare-system-1.0.0.jar");
                    string javafxLibPath = Path.Combine(projectRoot, "target", "javafx-lib");

                    if (File.Exists(jarPath) && Directory.Exists(javafxLibPath))
                    {
                        LaunchApplication(jarPath, javafxLibPath, projectRoot);
                    }
                    else
                    {
                        MessageBox.Show(
                            "Application files not found. Please run build-dist.ps1 first to build the application.",
                            "MediCare - Build Required",
                            MessageBoxButtons.OK,
                            MessageBoxIcon.Warning
                        );
                    }
                }
                else if (File.Exists(Path.Combine(exeDir, "medicare-system-1.0.0.jar")))
                {
                    // Running from bundled distribution
                    string jarPath = Path.Combine(exeDir, "medicare-system-1.0.0.jar");
                    LaunchApplication(jarPath, exeDir, exeDir);
                }
                else
                {
                    MessageBox.Show(
                        "MediCare application could not be located.\n\n" +
                        "Expected: " + Path.Combine(projectRoot, "target", "medicare-system-1.0.0.jar"),
                        "MediCare - Application Not Found",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    "Failed to start MediCare:\n\n" + ex.Message,
                    "MediCare - Startup Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
        }

        private static void LaunchApplication(string jarPath, string javafxLibPath, string workingDir)
        {
            try
            {
                if (!File.Exists(jarPath))
                {
                    MessageBox.Show(
                        "Application JAR not found:\n\n" + jarPath,
                        "MediCare - Application Not Found",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                if (!Directory.Exists(javafxLibPath))
                {
                    MessageBox.Show(
                        "JavaFX libraries not found:\n\n" + javafxLibPath,
                        "MediCare - Missing JavaFX",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                string javaExe = ResolveJavaExecutable("java");
                string javawExe = ResolveJavaExecutable("javaw");
                string launcherExe = string.IsNullOrWhiteSpace(javawExe) ? javaExe : javawExe;

                if (string.IsNullOrWhiteSpace(javaExe))
                {
                    MessageBox.Show(
                        "Java is not installed or not in system PATH.\n\n" +
                        "Please install JDK 17 or higher to run MediCare.",
                        "MediCare - Java Not Found",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                // Verify Java is installed
                ProcessStartInfo javaCheckInfo = new ProcessStartInfo
                {
                    FileName = javaExe,
                    Arguments = "-version",
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using (Process javaCheck = Process.Start(javaCheckInfo))
                {
                    if (javaCheck == null || !javaCheck.WaitForExit(5000))
                    {
                        MessageBox.Show(
                            "Java is not installed or not in system PATH.\n\n" +
                            "Please install JDK 17 or higher to run MediCare.",
                            "MediCare - Java Not Found",
                            MessageBoxButtons.OK,
                            MessageBoxIcon.Error
                        );
                        return;
                    }

                    if (javaCheck.ExitCode != 0)
                    {
                        MessageBox.Show(
                            "Java verification failed.\n\n" +
                            "Please ensure JDK 17 or higher is properly installed.",
                            "MediCare - Java Verification Failed",
                            MessageBoxButtons.OK,
                            MessageBoxIcon.Error
                        );
                        return;
                    }
                }

                // Launch the application
                ProcessStartInfo launchInfo = new ProcessStartInfo
                {
                    FileName = launcherExe,
                    Arguments = BuildJavaArguments(jarPath, javafxLibPath),
                    WorkingDirectory = workingDir,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardOutput = false,
                    RedirectStandardError = false
                };

                Process app = Process.Start(launchInfo);
                if (app == null)
                {
                    MessageBox.Show(
                        "Failed to start MediCare application process.",
                        "MediCare - Launch Failed",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                app.WaitForExit();
                while (app.ExitCode == 3)
                {
                    app = Process.Start(launchInfo);
                    if (app == null) return;
                    app.WaitForExit();
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    "Error launching MediCare:\n\n" + ex.Message,
                    "MediCare - Launch Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
        }

        private static string BuildJavaArguments(string jarPath, string javafxLibPath)
        {
            return "--module-path \"" + javafxLibPath + "\" " +
                   "--add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media " +
                   "--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED " +
                   "--add-opens javafx.web/com.sun.webkit=ALL-UNNAMED " +
                   "-jar \"" + jarPath + "\"";
        }

        private static string ResolveJavaExecutable(string exeName)
        {
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrWhiteSpace(javaHome))
            {
                string candidate = Path.Combine(javaHome, "bin", exeName + ".exe");
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }

            string path = Environment.GetEnvironmentVariable("PATH") ?? string.Empty;
            foreach (string part in path.Split(Path.PathSeparator))
            {
                if (string.IsNullOrWhiteSpace(part))
                {
                    continue;
                }

                string candidate = Path.Combine(part.Trim(), exeName + ".exe");
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }

            return null;
        }
    }
}


