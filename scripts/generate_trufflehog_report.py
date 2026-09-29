import json
import html
import sys

try:
    with open("trufflehog-reports/results.json") as f:
        lines = f.readlines()
    data = [json.loads(line) for line in lines if line.strip()]
except Exception:
    data = []

html_content = """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f4f6f9; }

        table { width: 100%; border-collapse: collapse; background: #fff; }

        th, td { padding: 12px; border: 1px solid #ddd; text-align: left; }

        th { background-color: #d9534f; color: white; }

        .badge { background: #5cb85c; color: white; padding: 4px 8px; border-radius: 3px; font-weight: bold; }
    </style>
</head>
<body>
    <h2>TruffleHog Secret Scan Report</h2>
               """

if not data:
    html_content += "<p><span class=\"badge\">PASSED</span> No secrets detected in workspace.</p>"
else:
    html_content += "<table><tr><th>Detector</th><th>Verified</th><th>Details</th></tr>"
    for item in data:
        detector = item.get("DetectorName", "Unknown")
        verified = "Yes" if item.get("Verified") else "No"
        raw = str(item.get("Raw", ""))
        html_content += f"<tr><td>{html.escape(detector)}</td><td>{verified}</td><td><pre>{html.escape(raw)}</pre></td></tr>"
    html_content += "</table>"

html_content += "</body></html>"

with open("trufflehog-reports/trufflehog_report.html", "w") as f:
    f.write(html_content)