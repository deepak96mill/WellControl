"""Finds tap positions in a `uiautomator dump` XML file.

  find_ui.py ui.xml switches        -> one "x y" line per toggle switch, top to bottom
  find_ui.py ui.xml text "Setup"    -> "x y" of the first element with that text
"""
import re
import sys
import xml.etree.ElementTree as ET


def nodes(path):
    out = []
    for n in ET.parse(path).iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if m:
            out.append((n,) + tuple(map(int, m.groups())))
    return out


ns = nodes(sys.argv[1])
mode = sys.argv[2]
if mode == "switches":
    screen_w = max(x2 for _, _, _, x2, _ in ns)
    found = []
    for n, x1, y1, x2, y2 in ns:
        w = x2 - x1
        if n.get("clickable") == "true" and w < 0.2 * screen_w and (x1 + x2) / 2 > 0.72 * screen_w:
            found.append(((x1 + x2) // 2, (y1 + y2) // 2))
    for x, y in sorted(set(found), key=lambda p: p[1]):
        print(x, y)
elif mode == "text":
    want = sys.argv[3]
    for n, x1, y1, x2, y2 in ns:
        if n.get("text") == want or n.get("content-desc") == want:
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
