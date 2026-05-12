Set shell = CreateObject("WScript.Shell")
shell.CurrentDirectory = "E:\DATN\project\boardinghouse_platform"
shell.Run """E:\DATN\project\boardinghouse_platform\run_manual_server.cmd""", 1, False
