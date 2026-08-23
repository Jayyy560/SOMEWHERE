from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.shape import WD_INLINE_SHAPE
from PIL import Image, ImageDraw, ImageFont
import math
import textwrap

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "synopsis_assets"
OUT = ROOT / "Somewhere_Project_Synopsis.docx"
ASSETS.mkdir(parents=True, exist_ok=True)

NAVY = "173A5E"
BLUE = "2E74B5"
TEAL = "1F7A8C"
INK = "17212B"
MUTED = "5B6770"
LIGHT = "EAF1F6"
LIGHTER = "F5F8FA"
WHITE = "FFFFFF"
GOLD = "B08B3E"


def rgb(hex_color):
    return RGBColor.from_string(hex_color)


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=90, start=120, bottom=90, end=120):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "1")
    tr_pr.append(tbl_header)


def set_table_widths(table, widths_inches):
    table.autofit = False
    tbl_pr = table._tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    total_dxa = round(sum(widths_inches) * 1440)
    tbl_w.set(qn("w:w"), str(total_dxa))
    tbl_w.set(qn("w:type"), "dxa")
    tbl_ind = tbl_pr.find(qn("w:tblInd"))
    if tbl_ind is None:
        tbl_ind = OxmlElement("w:tblInd")
        tbl_pr.append(tbl_ind)
    tbl_ind.set(qn("w:w"), "120")
    tbl_ind.set(qn("w:type"), "dxa")
    grid = table._tbl.tblGrid
    for child in list(grid):
        grid.remove(child)
    for width in widths_inches:
        col = OxmlElement("w:gridCol")
        col.set(qn("w:w"), str(round(width * 1440)))
        grid.append(col)
    for row in table.rows:
        for idx, width in enumerate(widths_inches):
            cell = row.cells[idx]
            cell.width = Inches(width)
            tc_pr = cell._tc.get_or_add_tcPr()
            tc_w = tc_pr.find(qn("w:tcW"))
            if tc_w is None:
                tc_w = OxmlElement("w:tcW")
                tc_pr.append(tc_w)
            tc_w.set(qn("w:w"), str(round(width * 1440)))
            tc_w.set(qn("w:type"), "dxa")
            set_cell_margins(cell)


def set_run_font(run, name="Calibri", size=11, color=INK, bold=False, italic=False):
    run.font.name = name
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), name)
    run.font.size = Pt(size)
    run.font.color.rgb = rgb(color)
    run.bold = bold
    run.italic = italic


def set_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = paragraph.add_run("Page ")
    set_run_font(run, size=9, color=MUTED)
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr_text)
    run._r.append(fld_char2)


def add_page_heading(doc, number, title, kicker=None):
    if kicker:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(5)
        r = p.add_run(kicker.upper())
        set_run_font(r, size=9, color=TEAL, bold=True)
        r.font.letter_spacing = Pt(1.5) if hasattr(r.font, "letter_spacing") else None
    p = doc.add_paragraph(style="Heading 1")
    p.paragraph_format.keep_with_next = True
    r = p.add_run(f"{number}. {title}")
    return p


def add_subheading(doc, text):
    p = doc.add_paragraph(style="Heading 2")
    p.paragraph_format.keep_with_next = True
    p.add_run(text)
    return p


def add_body(doc, text, after=5, first_indent=True):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = 1.12
    if first_indent:
        p.paragraph_format.first_line_indent = Inches(0.22)
    p.add_run(text)
    return p


def add_lead(doc, text):
    p = doc.add_paragraph(style="Lead")
    p.paragraph_format.space_after = Pt(9)
    p.add_run(text)
    return p


def add_bullet(doc, text, level=0):
    p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.10
    p.add_run(text)
    return p


def add_number(doc, text):
    p = doc.add_paragraph(style="List Number")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.10
    p.add_run(text)
    return p


def add_callout(doc, label, text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_widths(table, [6.5])
    cell = table.cell(0, 0)
    set_cell_shading(cell, LIGHTER)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    r = p.add_run(label.upper() + "  ")
    set_run_font(r, size=9.5, color=TEAL, bold=True)
    r = p.add_run(text)
    set_run_font(r, size=10.5, color=INK)


def add_table(doc, headers, rows, widths, font_size=9.2):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    set_table_widths(table, widths)
    hdr = table.rows[0]
    set_repeat_table_header(hdr)
    for i, header in enumerate(headers):
        cell = hdr.cells[i]
        set_cell_shading(cell, NAVY)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(0)
        r = p.add_run(header)
        set_run_font(r, size=font_size, color=WHITE, bold=True)
    for row_idx, values in enumerate(rows):
        cells = table.add_row().cells
        for i, value in enumerate(values):
            cell = cells[i]
            if row_idx % 2 == 1:
                set_cell_shading(cell, LIGHTER)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.08
            r = p.add_run(str(value))
            set_run_font(r, size=font_size, color=INK, bold=(i == 0))
    return table


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(8)
    r = p.add_run(text)
    set_run_font(r, size=9, color=MUTED, italic=True)
    return p


def add_image(doc, path, width=6.35, alt_text=None):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(0)
    inline = p.add_run().add_picture(str(path), width=Inches(width))
    description = alt_text or path.stem.replace("_", " ").title()
    inline._inline.docPr.set("descr", description)
    inline._inline.docPr.set("title", description)


def page_break(doc):
    doc.add_page_break()


W, H = 1560, 845
SX, SY = W / 12.0, H / 6.5
FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REGULAR, size)


def px(x, y):
    return int(x * SX), int(H - y * SY)


def diagram_base(title, subtitle=None):
    image = Image.new("RGB", (W, H), "white")
    draw = ImageDraw.Draw(image)
    draw.text(px(0.4, 6.12), title, font=font(42, True), fill="#173A5E", anchor="la")
    if subtitle:
        draw.text(px(0.4, 5.70), subtitle, font=font(22), fill="#5B6770", anchor="la")
    return image, draw


def box(draw, x, y, w, h, title, body="", fill="#F5F8FA", edge="#2E74B5", title_color="#173A5E"):
    left, bottom = px(x, y)
    right, top = px(x + w, y + h)
    rect = (left, top, right, bottom)
    draw.rounded_rectangle(rect, radius=12, fill=fill, outline=edge, width=3)
    tx, ty = left + 22, top + 20
    draw.text((tx, ty), title, font=font(25, True), fill=title_color, anchor="la")
    if body:
        width_chars = max(18, int(w * 16))
        lines = []
        for part in body.split("\n"):
            lines.extend(textwrap.wrap(part, width=width_chars) or [""])
        draw.multiline_text((tx, ty + 42), "\n".join(lines), font=font(18), fill="#37434D", spacing=7)


def arrow(draw, x1, y1, x2, y2, label=None):
    start = px(x1, y1)
    end = px(x2, y2)
    draw.line([start, end], fill="#1F7A8C", width=4)
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    length = 16
    wing = 0.58
    p1 = (end[0] - length * math.cos(angle - wing), end[1] - length * math.sin(angle - wing))
    p2 = (end[0] - length * math.cos(angle + wing), end[1] - length * math.sin(angle + wing))
    draw.polygon([end, p1, p2], fill="#1F7A8C")
    if label:
        mid = ((start[0] + end[0]) // 2, (start[1] + end[1]) // 2 - 14)
        draw.text(mid, label, font=font(17), fill="#1F7A8C", anchor="mm", stroke_width=3, stroke_fill="white")


def make_diagrams():
    # Architecture
    image, draw = diagram_base("System architecture", "Layered Android application with local and cloud data sources")
    box(draw, 0.5, 3.55, 3.1, 1.45, "Presentation layer", "Jetpack Compose screens, navigation, reusable components and theme system")
    box(draw, 4.45, 3.55, 3.1, 1.45, "State layer", "Hilt ViewModels expose immutable StateFlow UI state and process user events")
    box(draw, 8.4, 3.55, 3.1, 1.45, "Repository layer", "Drop, trip, image-match and AI repositories coordinate business operations")
    arrow(draw, 3.62, 4.28, 4.42, 4.28, "events / state")
    arrow(draw, 7.57, 4.28, 8.37, 4.28, "operations")
    box(draw, 0.7, 1.0, 2.25, 1.45, "Device services", "CameraX, fused location, sensors, ARCore and connectivity", fill="#EAF1F6")
    box(draw, 3.45, 1.0, 2.25, 1.45, "Room cache", "Account-scoped structured Drop cache and schema migrations", fill="#EAF1F6")
    box(draw, 6.2, 1.0, 2.25, 1.45, "Supabase", "Auth, Postgres, Storage and Realtime channels", fill="#EAF1F6")
    box(draw, 8.95, 1.0, 2.25, 1.45, "External services", "Maps/directions and optional AI-assisted summaries", fill="#EAF1F6")
    for x in (1.825, 4.575, 7.325, 10.075):
        arrow(draw, 9.95, 3.52, x, 2.5)
    image.save(ASSETS / "architecture.png")

    # Creation flow
    image, draw = diagram_base("Drop creation workflow", "Progressive disclosure keeps the core action short while preserving optional enhancements")
    steps = [
        (0.35, "1", "Capture", "Camera preview\nTake photo"),
        (2.65, "2", "Message", "Write up to 120 characters\nRetake remains available"),
        (4.95, "3", "Enhance", "Category, duration, audio, anonymity or attachment"),
        (7.25, "4", "Confirm place", "Collect stable fresh GPS samples\nShow reported accuracy"),
        (9.55, "5", "Publish", "Compress media, upload, insert metadata and cache locally"),
    ]
    for x, n, title, body in steps:
        box(draw, x, 2.05, 1.95, 2.45, f"{n}  {title}", body, fill="#F5F8FA")
    for i in range(len(steps)-1):
        arrow(draw, steps[i][0] + 1.97, 3.28, steps[i+1][0] - 0.03, 3.28)
    draw.text(px(0.55, 1.35), "Draft state is retained in SavedStateHandle; leaving after capture requires an explicit discard decision.",
              font=font(20, True), fill="#173A5E", anchor="la")
    draw.line([px(0.55, 1.12), px(11.35, 1.12)], fill="#1F7A8C", width=4)
    image.save(ASSETS / "creation_flow.png")

    # Discovery flow
    image, draw = diagram_base("Discovery and unlock workflow", "Location uncertainty is treated as part of the decision, not hidden from the user")
    box(draw, 0.45, 3.5, 2.1, 1.5, "Acquire context", "Fresh fused location, reported accuracy and rotation-vector heading")
    box(draw, 3.05, 3.5, 2.1, 1.5, "Query nearby", "Spatial RPC returns Drops within 500 m; local cache is merged")
    box(draw, 5.65, 3.5, 2.1, 1.5, "Calculate", "Distance, bearing and angular difference are recomputed")
    box(draw, 8.25, 3.5, 3.0, 1.5, "Render in camera", "ARCore pose when available; sensor-based spatial overlay remains the compatibility path")
    arrow(draw, 2.57, 4.25, 3.02, 4.25)
    arrow(draw, 5.17, 4.25, 5.62, 4.25)
    arrow(draw, 7.77, 4.25, 8.22, 4.25)
    box(draw, 2.0, 1.05, 3.2, 1.55, "Unlock decision", "Effective radius = 30 m + bounded combined uncertainty from current and placement fixes", fill="#EAF1F6")
    box(draw, 6.7, 1.05, 3.2, 1.55, "Persistent discovery", "Successful unlock is stored remotely and retained as a sticky local state", fill="#EAF1F6")
    arrow(draw, 6.7, 3.45, 4.4, 2.65, "distance + accuracy")
    arrow(draw, 5.25, 1.82, 6.65, 1.82, "within threshold")
    image.save(ASSETS / "discovery_flow.png")

    # Realtime sequence
    image, draw = diagram_base("Realtime interaction sequence", "Optimistic local feedback is reconciled with authoritative database events")
    actors = [(1.0, "User / UI"), (4.0, "ViewModel"), (7.0, "Repository"), (10.0, "Supabase")]
    for x, label in actors:
        draw.text(px(x, 5.3), label, font=font(24, True), fill="#173A5E", anchor="mm")
        draw.line([px(x, 0.7), px(x, 5.05)], fill="#B7C5D1", width=2)
    events = [
        (4.65, 1, 4, "submit comment"),
        (4.05, 4, 7, "validate + request"),
        (3.45, 7, 10, "insert row"),
        (2.85, 10, 7, "success / record"),
        (2.25, 7, 4, "emit updated comments"),
        (1.65, 4, 1, "recompose immediately"),
        (1.05, 10, 7, "Postgres change event → refresh"),
    ]
    for y, start, end, label in events:
        arrow(draw, start, y, end, y, label)
    image.save(ASSETS / "realtime_sequence.png")

    # GPS stabilisation
    image, draw = diagram_base("Stable placement algorithm", "The saved coordinate is based on a short sample window rather than one arbitrary fix")
    nodes = [
        (0.5, 3.75, 2.25, 1.25, "Request", "High-accuracy samples\n800 ms target interval"),
        (3.15, 3.75, 2.25, 1.25, "Reject", "Older than 10 s\nInvalid or >100 m accuracy"),
        (5.8, 3.75, 2.25, 1.25, "Estimate", "Accuracy-squared weighted geographic centre"),
        (8.45, 3.75, 2.85, 1.25, "Evaluate", "At least 4 samples, ≤20 m accuracy and ≤10 m spread"),
    ]
    for n in nodes:
        box(draw, *n)
    for i in range(len(nodes)-1):
        arrow(draw, nodes[i][0]+nodes[i][2]+0.03, 4.37, nodes[i+1][0]-0.03, 4.37)
    box(draw, 2.0, 1.1, 3.4, 1.35, "Stable before timeout", "Return the weighted coordinate with observed accuracy and provider metadata", fill="#EAF1F6")
    box(draw, 6.6, 1.1, 3.4, 1.35, "Ten-second timeout", "Return the best available estimate or display a retryable location state", fill="#EAF1F6")
    arrow(draw, 9.4, 3.7, 4.4, 2.5, "yes")
    arrow(draw, 9.8, 3.7, 8.4, 2.5, "no")
    image.save(ASSETS / "gps_algorithm.png")

    # Data model
    image, draw = diagram_base("Core data model", "A Drop is the central entity; social and discovery records reference it")
    box(draw, 4.3, 2.1, 3.4, 2.7, "DROP", "id • text • image/audio/file URL\nlatitude • longitude • created/expiry time\nauthor • category • anonymity\nplacement accuracy/time/provider\nmode flags: moment, dead drop, hitchhiker", fill="#EAF1F6", edge="#173A5E")
    relations = [
        (0.45, 4.05, "USER / PROFILE", "creates, blocks, reports"),
        (8.55, 4.05, "MEDIA STORAGE", "image, audio and attachment objects"),
        (0.45, 1.0, "UNLOCK RECORD", "user–drop discovery history"),
        (8.55, 1.0, "SOCIAL RECORDS", "comments, likes and notifications"),
    ]
    for x, y, title, body in relations:
        box(draw, x, y, 2.7, 1.25, title, body)
        arrow(draw, x + (2.7 if x < 4 else 0), y + 0.62, 4.25 if x < 4 else 7.75, 3.2)
    image.save(ASSETS / "data_model.png")


def setup_document():
    doc = Document()
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(0.72)
    section.bottom_margin = Inches(0.68)
    section.left_margin = Inches(1.0)
    section.right_margin = Inches(1.0)
    section.header_distance = Inches(0.42)
    section.footer_distance = Inches(0.42)
    section.different_first_page_header_footer = True

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    normal.font.size = Pt(10.5)
    normal.font.color.rgb = rgb(INK)
    normal.paragraph_format.space_after = Pt(5)
    normal.paragraph_format.line_spacing = 1.12

    for style_name, size, color, before, after in [
        ("Heading 1", 16.5, NAVY, 0, 8),
        ("Heading 2", 12.5, BLUE, 8, 4),
        ("Heading 3", 11.2, TEAL, 6, 3),
    ]:
        st = styles[style_name]
        st.font.name = "Calibri"
        st._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
        st._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
        st.font.size = Pt(size)
        st.font.bold = True
        st.font.color.rgb = rgb(color)
        st.paragraph_format.space_before = Pt(before)
        st.paragraph_format.space_after = Pt(after)
        st.paragraph_format.keep_with_next = True

    if "Lead" not in styles:
        lead = styles.add_style("Lead", WD_STYLE_TYPE.PARAGRAPH)
    else:
        lead = styles["Lead"]
    lead.font.name = "Calibri"
    lead._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    lead._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    lead.font.size = Pt(12.5)
    lead.font.color.rgb = rgb(NAVY)
    lead.font.italic = True
    lead.paragraph_format.line_spacing = 1.12

    for list_name in ("List Bullet", "List Bullet 2", "List Number"):
        st = styles[list_name]
        st.font.name = "Calibri"
        st._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
        st._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
        st.font.size = Pt(10.3)
        st.font.color.rgb = rgb(INK)
        st.paragraph_format.space_after = Pt(3)
        st.paragraph_format.line_spacing = 1.10

    header = section.header
    hp = header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.LEFT
    hr = hp.add_run("SOMEWHERE  /  PROJECT SYNOPSIS")
    set_run_font(hr, size=8.5, color=MUTED, bold=True)
    footer = section.footer
    set_page_number(footer.paragraphs[0])
    return doc


def build_document():
    make_diagrams()
    doc = setup_document()

    # PAGE 1 — TITLE
    doc.add_paragraph().paragraph_format.space_after = Pt(82)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("PROJECT SYNOPSIS")
    set_run_font(r, size=11, color=TEAL, bold=True)
    p.paragraph_format.space_after = Pt(18)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(12)
    r = p.add_run("SOMEWHERE")
    set_run_font(r, size=30, color=NAVY, bold=True)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(26)
    r = p.add_run("An Augmented-Reality and Location-Based Platform\nfor Preserving and Discovering Place-Based Digital Memories")
    set_run_font(r, size=16, color=INK, bold=False)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Submitted in partial fulfilment of the requirements for the degree of\nBachelor of Technology in Computer Science and Engineering")
    set_run_font(r, size=11, color=MUTED, italic=True)
    p.paragraph_format.space_after = Pt(34)
    meta = doc.add_table(rows=4, cols=2)
    meta.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta.style = "Table Grid"
    set_table_widths(meta, [2.1, 3.9])
    meta_rows = [
        ("Submitted by", "[Student Name]  |  [Roll Number]"),
        ("Project guide", "[Guide Name and Designation]"),
        ("Department", "Department of Computer Science and Engineering"),
        ("Institution / session", "[Institution Name]  |  [Academic Session]"),
    ]
    for i, (label, value) in enumerate(meta_rows):
        for j, text in enumerate((label, value)):
            cell = meta.cell(i, j)
            if j == 0:
                set_cell_shading(cell, LIGHT)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            rr = p.add_run(text)
            set_run_font(rr, size=10.5, color=INK, bold=(j == 0))
    doc.add_paragraph().paragraph_format.space_after = Pt(24)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Primary SDG: 11 — Sustainable Cities and Communities\nSupporting SDG: 9 — Industry, Innovation and Infrastructure")
    set_run_font(r, size=10, color=TEAL, bold=True)
    page_break(doc)

    # PAGE 2 — INTRODUCTION I
    add_page_heading(doc, "2", "INTRODUCTION", "Context and motivation")
    add_lead(doc, "Places carry stories, but most digital platforms detach those stories from the physical locations that give them meaning.")
    add_body(doc, "Mobile phones have become the primary interface through which people navigate cities, photograph experiences, communicate with communities and consume local information. Yet most location-aware applications treat a place as a coordinate, route endpoint, commercial listing or check-in. Personal memories, neighbourhood observations, local anecdotes and temporary moments are normally published into global feeds where the physical context becomes secondary. Once separated from place, a story is difficult for a future visitor to discover at the moment when it is most meaningful.")
    add_body(doc, "Somewhere is an Android application that explores a different model of digital communication: content is deliberately attached to a real-world location and becomes discoverable through proximity. A user creates a “Drop” by capturing a photograph, writing a short message and confirming the physical location. Other users explore nearby Drops through a camera-led discovery experience. Distance, direction and unlock state communicate that the content belongs to the environment rather than to an endless feed. The result is a community-authored layer of digital memories over streets, campuses, parks and other shared spaces.")
    add_subheading(doc, "2.1 Problem statement")
    add_body(doc, "The engineering problem is not merely to save latitude and longitude. A useful system must collect a fresh location despite GPS drift, preserve enough accuracy metadata to interpret later distance calculations, avoid showing distant content as local, orient multiple overlays without visual collision, synchronize reactions and comments, handle camera or network failure, and remain usable on devices that do not support every ARCore capability. It must also make sensitive actions—camera capture, location access, public posting and deletion—understandable and reversible wherever possible.")
    add_callout(doc, "Core proposition", "Convert physical proximity into a meaningful interaction rule: a Drop can be seen from nearby, but its full content is unlocked only after the visitor reaches the place within an accuracy-aware threshold.")
    add_body(doc, "The project therefore combines mobile user-experience design, geospatial computation, augmented reality, cloud data management and resilient Android architecture. Its value lies in how those parts cooperate. Camera imagery provides environmental context; location and heading establish spatial relevance; cloud services make the memory shareable; local persistence improves continuity; and state-driven interfaces explain what the system is doing.")
    page_break(doc)

    # PAGE 3 — INTRODUCTION II
    add_page_heading(doc, "2", "INTRODUCTION", "Scope and significance")
    add_subheading(doc, "2.2 Proposed system")
    add_body(doc, "The proposed system supports two principal journeys. In the creation journey, the user takes a photograph, writes a concise message, chooses optional enhancements and reviews the measured location before publishing. In the discovery journey, the application obtains the user’s current position and orientation, requests Drops within a bounded radius, calculates distance and bearing, and renders spatial cards over the live camera view. An unlock is recorded when the visitor enters an effective radius that combines the nominal proximity rule with bounded uncertainty from the current and original placement fixes.")
    add_body(doc, "The prototype includes account authentication, profiles, categories, permanent and expiring Drops, optional anonymous posting, audio or file attachments, likes, comments, notifications, user blocking and reporting. Wayfinder can interpret a destination or discovery prompt and lead the user toward relevant content. Place Summary and Whisper functions provide optional AI-assisted synthesis or curation of nearby public descriptions. Experimental Hitchhiker behaviour allows a Drop to be picked up and released at another location while preserving its journey; this is treated as an extension rather than the central requirement.")
    add_subheading(doc, "2.3 Significance")
    add_bullet(doc, "Place-based memory: the system preserves why a location mattered to a person, not only what services exist there.")
    add_bullet(doc, "Community discovery: users encounter local knowledge through movement and proximity instead of follower graphs or popularity-only ranking.")
    add_bullet(doc, "Technical integration: the project combines camera, GPS, sensors, AR rendering, local persistence and realtime cloud events in one coherent mobile workflow.")
    add_bullet(doc, "Responsible interaction: location accuracy, permissions, discard warnings, retry states and moderation controls are visible parts of the design.")
    add_subheading(doc, "2.4 Scope and constraints")
    add_body(doc, "The present implementation targets Android 8.0 and above and assumes a device with a camera, location services, accelerometer and compass. ARCore is optional at installation so unsupported devices can retain a camera-and-sensor discovery path; where ARCore tracking is available, device pose can improve spatial stability. The prototype focuses on short-form community memories rather than precise indoor navigation. GPS remains subject to multipath interference, poor sky visibility and device variation, so the interface communicates accuracy and applies bounded tolerance rather than claiming centimetre-level placement.")
    add_body(doc, "Somewhere aligns primarily with Sustainable Development Goal 11 because it encourages people to engage with local places and preserve community narratives. It supports SDG 9 by demonstrating an innovative digital layer that combines geospatial and AR infrastructure. These alignments describe the project’s intended contribution; they do not imply that the prototype alone solves urban sustainability.")
    page_break(doc)

    # PAGE 4 — RELATED WORKS I
    add_page_heading(doc, "3", "RELATED WORKS", "Foundations and precedents")
    add_subheading(doc, "3.1 Augmented reality as spatial registration")
    add_body(doc, "Azuma’s foundational survey defines augmented reality through the combination of real and virtual content, real-time interaction and registration in three dimensions [1]. This definition is important because a camera overlay alone is not sufficient: virtual content should respond consistently to viewpoint and environment. Modern ARCore operationalizes this principle using motion tracking, environmental understanding and light estimation. It combines visual feature points with inertial measurements to estimate camera pose and can render virtual content from the correct perspective [6]. Somewhere adopts this spatial objective while retaining a non-ARCore compatibility path, because the audience may use Android devices with different hardware certification and tracking quality.")
    add_subheading(doc, "3.2 Urban augmented guidance")
    add_body(doc, "Feiner, MacIntyre, Höllerer and Webster’s Touring Machine demonstrated a mobile augmented-reality system for exploring an urban environment [2]. It connected head-tracked graphics with location-aware information and showed that situated information could support movement through a city. The work established the relevance of direction, viewpoint and context long before smartphones made such experiences widely accessible. Somewhere differs in its emphasis on lightweight, community-created memories rather than institutional tour annotations, but it inherits the central idea that digital information becomes more useful when encountered where it belongs.")
    add_subheading(doc, "3.3 Geocaching and proximity-based discovery")
    add_body(doc, "Geocaching provides a mature precedent for coordinate-based community exploration. A participant hides a physical cache, publishes its coordinates and other players navigate using a GPS-enabled device; official guidance also stresses visiting the site and obtaining accurate coordinates [3]. This validates the motivational power of hidden, place-bound objects, but physical containers require permission, maintenance and space. Somewhere translates part of that behaviour into digital media. Its Drops can be permanent, temporary or experimental travelling objects, while the unlock record serves as a digital equivalent of finding and logging a cache.")
    add_subheading(doc, "3.4 Location-based games and movement")
    add_body(doc, "Pokémon GO demonstrated that a large-scale location-based application can motivate users to walk and explore. Althoff, White and Horvitz analysed activity and search signals for 32,000 users and reported a measurable short-term increase in physical activity associated with engagement [4]. This does not mean every location-based application produces health benefits, but it provides evidence that compelling spatial interaction can alter movement behaviour. Somewhere uses exploration as a pathway to stories rather than collection or combat mechanics, making its intended reward cultural and emotional rather than competitive.")
    page_break(doc)

    # PAGE 5 — RELATED WORKS II
    add_page_heading(doc, "3", "RELATED WORKS", "Comparative analysis and research gap")
    add_table(doc,
              ["System / work", "Primary interaction", "Strength", "Gap addressed by Somewhere"],
              [
                  ["Touring Machine [2]", "Urban AR guidance", "Situated information linked to viewpoint", "Community-authored, smartphone-scale memories and social interaction"],
                  ["Geocaching [3]", "Find physical containers using coordinates", "Strong sense of place and discovery", "No physical cache, media-rich content, expiry and accessibility controls"],
                  ["Pokémon GO [4]", "Location-based collection and AR game", "Motivates outdoor movement at scale", "User-generated local narratives rather than centralized game objects"],
                  ["Conventional social media", "Publish into feeds", "Easy creation and broad reach", "Physical context, proximity-based reveal and directional discovery"],
                  ["Map/review platforms", "Search places and businesses", "Structured navigation and utility", "Personal memory, ephemerality and camera-led experience"],
              ], [1.25, 1.55, 1.6, 2.1], font_size=8.5)
    add_caption(doc, "Table 1. Functional comparison of related systems")
    add_subheading(doc, "3.5 Research and design gap")
    add_body(doc, "Existing systems usually optimize one of three goals: navigation, entertainment or global social distribution. Few make the act of leaving a short multimedia memory at an ordinary location the primary interaction. A second gap is honesty about location quality. Many prototypes treat the coordinate as exact even though consumer GPS reports an uncertainty radius and may drift between creation and discovery. A third gap is cross-device resilience: an AR concept that depends entirely on a certified tracking stack excludes otherwise capable phones.")
    add_body(doc, "Somewhere responds with an accuracy-aware, layered design. At placement time, it gathers several fresh fused-location samples, rejects stale or unusable readings, estimates a weighted centre and stores accuracy, timestamp and provider metadata with the Drop. At discovery time, it combines the uncertainty of the current and placement fixes, bounds the additional tolerance and uses that value in the unlock decision. The system also limits and fans out co-located overlays to reduce clutter, keeps previously recorded unlocks stable, and provides retry states for location, network and camera failure.")
    add_subheading(doc, "3.6 Architectural foundation")
    add_body(doc, "The application follows Android’s recommended separation between UI and data responsibilities. Compose screens render immutable state exposed by ViewModels; repositories coordinate Room, device services and Supabase. Android’s architecture guidance recommends repositories, unidirectional data flow and persistent models as a single source of truth [5]. Room is suitable for a structured local cache and supports compile-time query validation and migration paths [9]. Supabase Realtime enables subscriptions to PostgreSQL changes [8], allowing comments, likes, Drops and notifications to be reconciled without fixed-delay refresh logic.")
    add_callout(doc, "Identified gap", "A community-authored AR memory layer must be spatially meaningful, honest about uncertainty, socially responsive and resilient across heterogeneous Android devices—not simply visually placed over a camera preview.")
    page_break(doc)

    # PAGE 6 — OBJECTIVES: STUDY I
    add_page_heading(doc, "4", "OBJECTIVES", "A. Study")
    add_lead(doc, "The study objectives establish the human, spatial and technical requirements before implementation decisions are made.")
    add_subheading(doc, "4.1 Study of user behaviour and place-based interaction")
    add_number(doc, "Examine how users currently preserve and share memories through photographs, captions, maps and social feeds, and identify where physical context is lost.")
    add_number(doc, "Study the motivation behind local exploration, hidden-object discovery and proximity-based rewards without assuming that competitive game mechanics are required.")
    add_number(doc, "Identify the minimum information needed for a Drop to feel meaningful: a visual anchor, concise message, creator identity or anonymity, time behaviour and physical place.")
    add_number(doc, "Study privacy expectations around camera, precise location, background location, photographs, notifications and public comments.")
    add_body(doc, "This part of the study frames the application as a situated communication system rather than a conventional media feed. The central hypothesis is that scarcity and context can increase meaning: a short message discovered at the place where it was left may carry more significance than the same message encountered randomly online. User evaluation should therefore assess not only task completion but also whether people understand why content is locked, whether they trust the displayed distance and accuracy, and whether the camera interface encourages safe environmental awareness.")
    add_subheading(doc, "4.2 Study of geospatial uncertainty")
    add_bullet(doc, "Compare last-known location with fresh high-accuracy requests and continuous updates.")
    add_bullet(doc, "Observe the effect of sample age, reported horizontal accuracy, provider and environmental conditions.")
    add_bullet(doc, "Evaluate distance using Android’s geodesic calculation and bearing using latitude/longitude trigonometry.")
    add_bullet(doc, "Determine a practical discovery radius, unlock radius, hysteresis margin and maximum number of visible overlays.")
    add_body(doc, "Consumer positioning does not identify an exact point; each measurement represents an estimate with error. The study therefore treats placement and discovery as two uncertain observations. Their combined uncertainty can be approximated by the square root of the sum of squared accuracy values. Only a bounded part of that uncertainty is added to the physical unlock radius, preventing poor fixes from unlocking content from an unreasonable distance. This approach is designed for predictable behaviour, not for surveying-grade precision.")
    page_break(doc)

    # PAGE 7 — OBJECTIVES: STUDY II / DESIGN
    add_page_heading(doc, "4", "OBJECTIVES", "A. Study and B. Design")
    add_subheading(doc, "4.3 Study of mobile AR and device compatibility")
    add_body(doc, "Study ARCore motion tracking, surface understanding and camera-pose estimation, together with the limitations of device certification, lighting, texture, movement and tracking loss. Compare true pose-based anchoring with heading-based screen overlays. The design goal is progressive capability: ARCore should improve embodiment when available, while basic discovery should remain possible through the camera, compass and GPS when ARCore cannot initialize.")
    add_subheading(doc, "4.4 Study of data synchronization and failure")
    add_body(doc, "Investigate how user-generated media, structured metadata and realtime social events should be divided between object storage, PostgreSQL records and local persistence. Study optimistic updates for immediate feedback, followed by server reconciliation. Identify failure cases including offline operation, request timeout, stale cache, rejected upload, deleted content, expired sessions and lifecycle interruption during camera or location work.")
    add_subheading(doc, "4.5 Design objectives")
    add_number(doc, "Design a progressive creation flow—photo, message, optional enhancements and location confirmation—so the essential task remains clear.")
    add_number(doc, "Design a camera-led discovery interface that communicates direction, distance, category, locked state and selection without allowing overlays to collapse into an unreadable cluster.")
    add_number(doc, "Design visual hierarchy using a consistent wordmark-derived type system, restrained buttons, clear selected states and standard top app bars.")
    add_number(doc, "Design transparent system feedback: real loading states, current GPS accuracy, retry actions and contextual permission education.")
    add_number(doc, "Design draft preservation and explicit discard confirmation so a captured photograph or message is not lost accidentally.")
    add_number(doc, "Design moderation and account controls, including reporting, blocking, deletion and deep-linked notifications.")
    add_callout(doc, "Design principle", "The interface should reveal complexity progressively. Camera, location and cloud operations remain visible enough to build trust, but optional features must not compete with the primary creation and discovery tasks.")
    add_body(doc, "The design uses reusable Compose components and state-driven rendering so each asynchronous operation has a defined idle, loading, success and failure state. It avoids fixed delays for refresh indicators and prevents visual state from becoming detached from actual work. Navigation labels are explicit, and Wayfinder, Home, Create and Profile have distinct destinations and selected states.")
    page_break(doc)

    # PAGE 8 — OBJECTIVES: DESIGN II
    add_page_heading(doc, "4", "OBJECTIVES", "B. Design")
    add_subheading(doc, "4.6 Functional design")
    add_table(doc,
              ["Module", "Design objective", "Acceptance indicator"],
              [
                  ["Authentication", "Provide secure email account creation, sign-in and password recovery", "Session is restored and account-scoped content remains separated"],
                  ["Drop creation", "Capture media, message, enhancements and verified location", "Draft survives navigation; accuracy appears before publish"],
                  ["Discovery", "Retrieve and spatially arrange nearby Drops", "Distance/bearing update with movement; clutter is bounded"],
                  ["Unlocking", "Reveal content only near its place with uncertainty tolerance", "Decision uses current and placement accuracy metadata"],
                  ["Social", "Support comments, likes and relevant notifications", "Local action updates immediately and remote events reconcile"],
                  ["Wayfinder", "Interpret nearby, destination and surprise intents", "Route state reflects the current operation and failure is retryable"],
                  ["Profile", "Separate created, discovered and carried content", "Account data is synchronized and cached predictably"],
              ], [1.25, 3.35, 1.9], font_size=8.6)
    add_caption(doc, "Table 2. Functional design objectives and observable indicators")
    add_subheading(doc, "4.7 Non-functional design")
    add_bullet(doc, "Usability: core actions must be recognizable without relying on icon glow or colour alone.")
    add_bullet(doc, "Reliability: the app must expose network, camera and location failures rather than silently closing or hanging.")
    add_bullet(doc, "Performance: nearby results are limited, media is compressed before upload, and sensor-driven overlay updates are throttled.")
    add_bullet(doc, "Maintainability: concerns are divided into UI, ViewModel, repository, local, remote, location and utility packages.")
    add_bullet(doc, "Compatibility: ARCore remains optional and CameraX provides lifecycle-aware camera use cases across varied Android hardware.")
    add_bullet(doc, "Security and ethics: secrets are configured outside source code, user sessions are authenticated, and public content can be reported or hidden.")
    add_body(doc, "Accessibility and safety are considered alongside visual quality. Text must retain readable contrast over a live camera feed, touch targets must remain usable, state changes must have labels, and interaction should not encourage prolonged attention while walking. The application should educate users at the point where camera or location access becomes relevant instead of presenting an abstract permission lecture before they understand the benefit.")
    page_break(doc)

    # PAGE 9 — OBJECTIVES: IMPLEMENTATION
    add_page_heading(doc, "4", "OBJECTIVES", "C. Implementation")
    add_subheading(doc, "4.8 Core implementation objectives")
    add_number(doc, "Implement a single-activity Android application in Kotlin using Jetpack Compose and Navigation Compose.")
    add_number(doc, "Implement lifecycle-aware state holders with ViewModel, StateFlow, coroutines and SavedStateHandle.")
    add_number(doc, "Implement CameraX preview, capture, tap-to-focus and device-level camera error recovery.")
    add_number(doc, "Implement fresh fused-location collection, weighted coordinate estimation, accuracy storage and timeout handling.")
    add_number(doc, "Implement distance, bearing, heading smoothing, overlay fan-out, hysteresis and accuracy-aware unlocking.")
    add_number(doc, "Implement ARCore/SceneView tracking where supported and preserve a compatibility discovery path.")
    add_number(doc, "Implement Supabase authentication, PostgreSQL operations, Storage uploads and Realtime subscriptions.")
    add_number(doc, "Implement Room caching with explicit schema migrations and account-scoped queries.")
    add_number(doc, "Implement reactions, realtime comments, notifications, reporting, blocking and deep-link navigation.")
    add_number(doc, "Implement observable loading/error/offline states and verify the application on multiple physical devices.")
    add_subheading(doc, "4.9 Prototype extensions")
    add_body(doc, "The implementation also investigates optional extensions without allowing them to define the minimum viable system. Moments expire after a chosen duration; Dead Drops attach a downloadable file; Ghost mode hides author identity; Hitchhiker Drops may move between locations while recording journey stops; Wayfinder processes natural-language discovery prompts; and AI-assisted Summary or Whisper functions synthesize nearby public text. Feature flags allow experimental components to be excluded from a demonstration while their code remains available for evaluation.")
    add_subheading(doc, "4.10 Verification objectives")
    add_bullet(doc, "Unit-test deterministic location and theme-persistence logic.")
    add_bullet(doc, "Test creation, cancellation, process recreation and draft recovery.")
    add_bullet(doc, "Test discovery under strong, moderate and poor reported GPS accuracy.")
    add_bullet(doc, "Test AR-supported and non-supported devices, including camera initialization failure.")
    add_bullet(doc, "Test realtime comments on separate accounts and devices, including reconnection.")
    add_bullet(doc, "Compile release-like builds with resource shrinking and inspect runtime logs for uncaught exceptions.")
    add_callout(doc, "Completion criterion", "A user can create a location-aware Drop, another user can discover and unlock it near the correct place, and both users receive coherent social updates without losing drafts or encountering an unexplained application exit.")
    page_break(doc)

    # PAGE 10 — METHODOLOGY: DEVELOPMENT APPROACH
    add_page_heading(doc, "5", "METHODOLOGY", "Development approach")
    add_subheading(doc, "5.1 Iterative and evidence-driven process")
    add_body(doc, "The project follows an iterative prototyping methodology. Each cycle begins with a user or system problem, reproduces it under controlled conditions, identifies the responsible layer, implements a bounded correction and verifies the result on a build or physical device. This approach is appropriate because camera, sensor and location behaviour cannot be validated entirely in an emulator, while interface quality requires repeated observation rather than one-time specification.")
    add_number(doc, "Discover: collect workflow observations, screenshots, user reports, crash symptoms and location behaviour.")
    add_number(doc, "Define: translate each symptom into a testable requirement, such as “comments appear immediately after submission” or “placement displays current accuracy before publishing.”")
    add_number(doc, "Design: create the smallest coherent workflow and state model, including error and cancellation paths.")
    add_number(doc, "Implement: change the relevant Compose, ViewModel, repository or service layer while preserving unrelated behaviour.")
    add_number(doc, "Verify: run unit tests and debug builds, inspect logs, and test on devices with different manufacturers and ARCore support.")
    add_number(doc, "Refine: use the observed result to correct hierarchy, timing, copy, state persistence or spatial calculation.")
    add_subheading(doc, "5.2 Requirements prioritization")
    add_body(doc, "Requirements are prioritized by user harm and workflow criticality. Application exits, content loss, incorrect unlocks and invisible text are treated as defects. Misleading loading indicators, weak navigation states and ambiguous labels are addressed next because they undermine trust. Experimental features and aesthetic themes are developed only after the core creation and discovery flows remain stable.")
    add_table(doc,
              ["Priority", "Examples", "Verification"],
              [
                  ["Critical", "Crash, failed publish, lost draft, wrong-account data", "Device logs, end-to-end task, account switching"],
                  ["High", "Incorrect distance/unlock, stale comments, permission dead end", "Location scenarios, two-device realtime test"],
                  ["Medium", "Hierarchy, loading feedback, empty/error states", "Screen review and state transition tests"],
                  ["Exploratory", "Hitchhiker, AI curation, alternate theme", "Feature flag and bounded prototype evaluation"],
              ], [1.0, 3.55, 1.95], font_size=8.7)
    add_caption(doc, "Table 3. Requirement prioritization used during iterative development")
    page_break(doc)

    # PAGE 11 — ARCHITECTURE
    add_page_heading(doc, "5", "METHODOLOGY", "System architecture")
    add_image(doc, ASSETS / "architecture.png", width=6.45)
    add_caption(doc, "Figure 1. Layered architecture of Somewhere")
    add_body(doc, "The presentation layer contains Compose screens for authentication, Home, Drop creation, Discovery, Wayfinder, Profile, notifications and settings. Reusable components provide app bars, buttons, navigation, camera background, permission gates and Drop cards. Compose observes immutable StateFlow values, so UI refresh is linked to operation state rather than manual fixed delays.")
    add_body(doc, "ViewModels retain screen state across recomposition, run asynchronous operations in viewModelScope and convert repository results into explicit UI states. SavedStateHandle preserves the creation draft, including the captured image URI, message, category, duration, audio, attachment and current progressive step. Hilt supplies repositories, Room DAO, sensor manager, fused-location client and network services.")
    add_body(doc, "DropRepository acts as the principal coordination point for Drop operations. It compresses and uploads media, inserts metadata, merges remote nearby results with the account-scoped Room cache, calculates filters, records unlocks, and exposes realtime change flows. Supabase provides authenticated sessions, PostgreSQL records, object storage and realtime subscriptions. Room provides a local structured cache and a sequence of explicit migrations as the Drop model evolves.")
    add_callout(doc, "Architectural rationale", "The Activity hosts the application; it does not own business data. State and data responsibilities remain in ViewModels and repositories, supporting lifecycle recovery, testing and incremental replacement of individual services.")
    page_break(doc)

    # PAGE 12 — CREATION FLOW
    add_page_heading(doc, "5", "METHODOLOGY", "Drop creation workflow")
    add_image(doc, ASSETS / "creation_flow.png", width=6.45)
    add_caption(doc, "Figure 2. Progressive Drop creation workflow")
    add_subheading(doc, "5.3 Capture and message")
    add_body(doc, "CameraX binds preview and image capture to the lifecycle. The user captures a photograph or receives a retryable camera state if initialization fails. The resulting content URI is stored in the ViewModel and SavedStateHandle. The message step accepts a concise description up to 120 characters and keeps Retake accessible because the image and text must be reviewed together. Navigating away does not silently discard the draft; a destructive discard requires confirmation.")
    add_subheading(doc, "5.4 Enhancements and location confirmation")
    add_body(doc, "Enhancements are optional and appear after the required message. They include permanent or expiring behaviour, category, anonymity, recorded audio, file attachment and experimental Hitchhiker mode. The final step collects fresh high-accuracy location samples for up to ten seconds. Current accuracy is displayed before the primary “Mark This Place” action, allowing the user to wait, move toward open sky or retry.")
    add_subheading(doc, "5.5 Persistence")
    add_body(doc, "Before upload, the photograph is decoded, scaled to a maximum dimension of 1024 pixels and compressed as JPEG to reduce transfer cost. Media is written to Supabase Storage using account-namespaced object paths. A PostgreSQL Drop record stores text, media URLs, coordinate, creation and expiry time, author settings, category and placement-quality metadata. The successfully created model is also inserted into Room so the creator’s profile updates promptly.")
    page_break(doc)

    # PAGE 13 — GPS ALGORITHM
    add_page_heading(doc, "5", "METHODOLOGY", "Location stabilization and proximity logic")
    add_image(doc, ASSETS / "gps_algorithm.png", width=6.4)
    add_caption(doc, "Figure 3. Stable placement algorithm")
    add_subheading(doc, "5.6 Stable coordinate estimation")
    add_body(doc, "A single GPS callback can be stale or temporarily displaced. The collector therefore requests high-accuracy samples, rejects readings older than ten seconds and ignores missing, non-finite or excessively poor accuracy. Up to twelve recent valid samples are maintained. Each sample receives a weight inversely proportional to the square of its reported accuracy; latitude and longitude are averaged using these weights. The resulting accuracy never claims to be better than the best source reading and is increased when the observed sample spread is larger.")
    add_body(doc, "The estimate is accepted early after at least four samples when reported accuracy is no worse than 20 metres and the 75th-percentile sample spread is no more than 10 metres. Otherwise the best estimate is returned at the ten-second timeout. The saved Drop includes placementAccuracyMeters, placementLocationTimestamp and placementLocationProvider. This metadata makes the later unlock decision auditable and enables the UI to distinguish a coordinate from its quality.")
    add_subheading(doc, "5.7 Distance and unlock")
    add_body(doc, "Discovery calculates geodesic distance between the current and Drop coordinates and derives bearing from spherical trigonometry. The nominal unlock radius is 30 metres. Current and placement uncertainty are combined as √(a²+b²); at most 25 metres of this value may extend the radius. Thus, moderate error can prevent two measurements of the same place from contradicting each other, while very poor accuracy cannot open Drops from hundreds of metres away. A 20-metre hysteresis margin prevents nearby overlays from repeatedly appearing and disappearing at the discovery boundary.")
    add_callout(doc, "Important limitation", "Reported GPS accuracy is an estimate, not a guarantee. The algorithm improves consistency and honesty but cannot remove multipath error, indoor attenuation or incorrect sensor calibration.")
    page_break(doc)

    # PAGE 14 — DISCOVERY / AR
    add_page_heading(doc, "5", "METHODOLOGY", "Discovery and augmented-reality workflow")
    add_image(doc, ASSETS / "discovery_flow.png", width=6.42)
    add_caption(doc, "Figure 4. Nearby retrieval, orientation and unlock flow")
    add_subheading(doc, "5.8 Spatial retrieval and presentation")
    add_body(doc, "The discovery screen begins location and rotation-vector tracking after permission is granted. A fresh current location starts the first query, followed by continuous updates. To avoid unnecessary backend traffic, positions are updated on every useful reading while nearby data is refetched only after meaningful movement, elapsed time or a realtime change event. The repository invokes a spatial PostgreSQL function with the current coordinate, a 500-metre radius and a result limit. Remote results are merged with relevant local data, filtered for expiry, reporting and blocking, sorted by distance and limited to fifteen visible overlays.")
    add_body(doc, "For every Drop, the ViewModel calculates bearing and the signed angular difference from the device heading. The rotation vector is smoothed while correctly handling the transition between 359° and 0°, and visual updates are throttled to approximately 30 Hz. Co-located Drops are deterministically fanned out rather than stacked at one coordinate. The closest item receives primary treatment; selected or newly unlocked items receive explicit states rather than relying only on animation.")
    add_subheading(doc, "5.9 ARCore and compatibility")
    add_body(doc, "When ARCore tracking is available, the renderer can use device pose and environmental understanding to improve the impression that content remains in the environment. Tracking loss, unavailable services or an unsupported phone must not terminate the main application. ARCore is therefore declared optional, and a sensor-based camera overlay remains the fallback. This trade-off preserves reach while acknowledging that the fallback cannot provide the same world-locked stability or occlusion as a fully tracked anchor.")
    add_body(doc, "The camera HUD includes category filtering, list access, nearby counts, Summary and Whisper actions, plus permission, location-timeout, network-error and camera-unavailable states. Full-image viewing uses a deliberate hold gesture only on the unobstructed image area so comment and action overlays remain interactive.")
    page_break(doc)

    # PAGE 15 — DATA MODEL / STACK
    add_page_heading(doc, "5", "METHODOLOGY", "Data model and technology stack")
    add_image(doc, ASSETS / "data_model.png", width=6.35)
    add_caption(doc, "Figure 5. Core logical data model")
    add_table(doc,
              ["Layer", "Technology", "Role in the system"],
              [
                  ["Language / runtime", "Kotlin, Java 17", "Application logic, coroutines and type-safe models"],
                  ["User interface", "Jetpack Compose, Material 3", "Declarative screens, state-driven rendering and reusable components"],
                  ["Architecture", "ViewModel, StateFlow, Hilt", "State retention, unidirectional flow and dependency injection"],
                  ["Camera / AR", "CameraX, ARCore, SceneView", "Preview, image capture, focus and spatial rendering"],
                  ["Location", "Fused Location Provider, sensors", "Fresh position, accuracy, heading and background trip context"],
                  ["Local data", "Room / SQLite", "Account-scoped cache and migrations"],
                  ["Cloud", "Supabase Auth, Postgres, Storage, Realtime", "Identity, structured records, media and live events"],
                  ["Networking / media", "Ktor, Retrofit, OkHttp, Coil", "HTTP services, diagnostics and image loading"],
                  ["Quality", "JUnit, Sentry, Gradle", "Unit verification, crash diagnosis and repeatable builds"],
              ], [1.2, 2.1, 3.2], font_size=8.35)
    add_caption(doc, "Table 4. Main implementation technologies")
    add_body(doc, "The Drop entity is versioned locally through Room migrations and mirrored to cloud records. Its placement-quality fields were added as a schema evolution rather than replacing existing data. Social and discovery records reference Drop identifiers, allowing likes, comments, notifications and unlock histories to evolve independently of the media object. Object storage carries large binary content while PostgreSQL retains searchable metadata.")
    page_break(doc)

    # PAGE 16 — REALTIME, SECURITY, TESTING
    add_page_heading(doc, "5", "METHODOLOGY", "Realtime interaction, security and testing")
    add_image(doc, ASSETS / "realtime_sequence.png", width=6.3)
    add_caption(doc, "Figure 6. Comment update and realtime reconciliation sequence")
    add_subheading(doc, "5.10 Realtime and resilience")
    add_body(doc, "Comments and likes update through a combination of immediate local state and Supabase change subscriptions. The local action is reflected as soon as the authoritative request succeeds, without waiting for the same device to receive its own database event. Realtime events refresh other clients and reconcile the final server state. Loading indicators remain bound to active coroutine work, and errors expose retry actions. Cached nearby Drops may be used when a remote read temporarily fails, while publish operations report failure rather than pretending completion.")
    add_subheading(doc, "5.11 Security, privacy and moderation")
    add_body(doc, "Authentication uses Supabase email sessions. Service URLs and keys are supplied through local build configuration rather than hard-coded business logic. The app requests camera and location permissions contextually and provides explicit reasons. Media is stored under generated account paths. Users can report a Drop, block an author, delete their own content and manage permissions. Production deployment should enforce PostgreSQL row-level security and storage policies, remove verbose HTTP body logging, audit public bucket exposure, minimize background-location use and publish clear retention and moderation policies.")
    add_subheading(doc, "5.12 Verification plan")
    add_table(doc,
              ["Test area", "Representative case", "Expected result"],
              [
                  ["Creation", "Leave and return after photo/message", "Draft restored; discard is explicit"],
                  ["Location", "Create and discover at same place with 10–35 m accuracy", "Drop unlocks within bounded effective radius"],
                  ["Compatibility", "ARCore unsupported or unavailable", "Fallback discovery works; no process exit"],
                  ["Realtime", "Comment from a second account", "Both clients update without reopening the Drop"],
                  ["Failure", "Disable network, camera or location", "Clear state and retry action; no indefinite loading"],
                  ["Moderation", "Report/block/delete", "Content disappears appropriately and backend result is respected"],
              ], [1.15, 2.7, 2.65], font_size=8.5)

    # PAGE 17 — OUTCOMES I
    add_page_heading(doc, "6", "OUTCOMES", "Expected technical and societal outcomes")
    add_subheading(doc, "6.1 Functional outcome")
    add_body(doc, "The expected outcome is a working Android prototype in which authenticated users create multimedia Drops at measured real-world locations and other users discover them through a camera-led, direction-aware interface. The prototype should retain creation drafts, disclose placement accuracy, calculate nearby distance and bearing, persist unlock history, synchronize social interactions and recover from ordinary camera, location and network failures.")
    add_subheading(doc, "6.2 Technical outcome")
    add_bullet(doc, "A maintainable, single-activity Compose application with explicit state holders and repository boundaries.")
    add_bullet(doc, "An accuracy-aware geospatial pipeline that stores both coordinates and measurement-quality metadata.")
    add_bullet(doc, "An AR-enhanced discovery experience with a compatibility path for devices that cannot start ARCore.")
    add_bullet(doc, "A hybrid persistence strategy using Room for local structured continuity and Supabase for shared cloud state.")
    add_bullet(doc, "Realtime comments, reactions, notifications and discovery refreshes driven by actual events.")
    add_bullet(doc, "A reusable basis for campus storytelling, neighbourhood archives, local tours and temporary events.")
    add_subheading(doc, "6.3 SDG alignment")
    add_table(doc,
              ["SDG", "Alignment", "Project contribution"],
              [
                  ["SDG 11 — Sustainable Cities and Communities", "Primary", "Supports local exploration and community-authored preservation of place-based cultural and personal narratives"],
                  ["SDG 9 — Industry, Innovation and Infrastructure", "Supporting", "Demonstrates innovative integration of AR, geospatial computing, cloud data and mobile infrastructure"],
                  ["SDG 3 — Good Health and Well-being", "Indirect", "Outdoor discovery may encourage walking and social connection; the prototype does not claim a measured health intervention"],
                  ["SDG 4 — Quality Education", "Potential", "The platform can support campus, heritage and location-based learning when educational content is intentionally curated"],
              ], [1.75, 0.85, 3.9], font_size=8.55)
    add_caption(doc, "Table 6. Sustainable Development Goal mapping")
    add_callout(doc, "Primary claim", "The defensible sustainability contribution is SDG 11, supported by technological innovation under SDG 9. Other benefits remain indirect or future possibilities until evaluated.")
    page_break(doc)

    # PAGE 18 — OUTCOMES II + REFERENCES
    add_page_heading(doc, "6", "OUTCOMES", "PO/PSO mapping, limitations and references")
    add_table(doc,
              ["Outcome", "Project evidence"],
              [
                  ["PO3 — Design solutions", "Designs a multi-stage location-aware system with usability, safety, privacy and societal considerations"],
                  ["PO5 — Modern tool usage", "Applies Kotlin, Compose, CameraX, ARCore, Room, Supabase, sensors, cloud storage and diagnostic tools"],
                  ["PO7 — Sustainability", "Connects digital innovation with community exploration and place-based memory under SDG 11"],
                  ["PSO2 — Systematic solution", "Uses layered architecture, stable-location sampling, bounded uncertainty and state-based failure recovery"],
                  ["PSO4 — Technical competence", "Integrates mobile, database, networking, AR, geospatial and realtime engineering"],
              ], [2.05, 4.45], font_size=8.45)
    add_caption(doc, "Table 7. Primary Program Outcome and Program Specific Outcome mapping")
    add_subheading(doc, "6.4 Limitations and future outcomes")
    add_body(doc, "The present system depends on consumer GPS and cannot guarantee indoor or centimetre-level placement. Camera-only fallback overlays are less world-locked than ARCore anchors, and full environmental realism would require better plane anchoring, depth-based occlusion, lighting response and persistent geospatial anchors. Public deployment also requires stronger moderation operations, formal privacy review, production row-level security, scalable realtime broadcast design and systematic field evaluation. Future work may add curated heritage collections, accessibility modes, privacy-preserving location disclosure and cross-platform support.", after=5)
    add_subheading(doc, "References")
    refs = [
        "[1] R. T. Azuma, “A Survey of Augmented Reality,” Presence: Teleoperators and Virtual Environments, vol. 6, no. 4, pp. 355–385, 1997. doi: 10.1162/pres.1997.6.4.355.",
        "[2] S. Feiner, B. MacIntyre, T. Höllerer and A. Webster, “A Touring Machine: Prototyping 3D Mobile Augmented Reality Systems for Exploring the Urban Environment,” Proc. ISWC, 1997.",
        "[3] Geocaching, “Fast Facts” and official cache placement guidance, Geocaching.com, accessed August 2026.",
        "[4] T. Althoff, R. W. White and E. Horvitz, “Influence of Pokémon Go on Physical Activity: Study and Implications,” Journal of Medical Internet Research, vol. 18, no. 12, e315, 2016. doi: 10.2196/jmir.6759.",
        "[5] Android Developers, “Guide to App Architecture,” Google, accessed August 2026.",
        "[6] Google for Developers, “Overview of ARCore” and “Fundamental Concepts,” accessed August 2026.",
        "[7] Android Developers, “CameraX Architecture” and “Retrieve Current Location,” accessed August 2026.",
        "[8] Supabase, “Realtime: Postgres Changes,” accessed August 2026.",
        "[9] Android Developers, “Save Data in a Local Database Using Room,” accessed August 2026.",
    ]
    for ref in refs:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.18)
        p.paragraph_format.first_line_indent = Inches(-0.18)
        p.paragraph_format.space_after = Pt(2.5)
        p.paragraph_format.line_spacing = 1.0
        r = p.add_run(ref)
        set_run_font(r, size=7.8, color=INK)

    # Metadata and core properties
    doc.core_properties.title = "Somewhere — Project Synopsis"
    doc.core_properties.subject = "B.Tech. Computer Science and Engineering Project Synopsis"
    doc.core_properties.author = "[Student Name]"
    doc.core_properties.keywords = "augmented reality, location-based services, Android, geospatial, Supabase, SDG 11"

    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    build_document()
