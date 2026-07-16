import os
import re

template_dir = r"C:\Users\user\.gemini\antigravity\scratch\GhanaRide\src\main\resources\templates"

for root, _, files in os.walk(template_dir):
    for file in files:
        if file.endswith(".html"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()

            new_content = content
            
            # Replace inline style="background: #1a1a2e;" with class="... bg-dark"
            # It's safer to just remove it and rely on bg-dark if not present, but replacing `#1a1a2e` with `var(--card-bg)` works everywhere.
            new_content = new_content.replace('background: #1a1a2e', 'background: var(--card-bg)')
            
            # Remove table-dark
            new_content = new_content.replace('table-dark', '')
            # Clean up extra spaces
            new_content = new_content.replace('table  table-hover', 'table table-hover')
            new_content = new_content.replace('table table-hover', 'table table-hover')
            
            if new_content != content:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Updated {path}")
