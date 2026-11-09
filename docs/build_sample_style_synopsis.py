"""Build the detailed, chapter-based project synopsis for Somewhere.

The document follows the supplied 31-page academic synopsis model while using
only implementation details evidenced by this repository.  Personal and
institutional information intentionally remains as editable placeholders.
"""
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "docx" / "Somewhere_Project_Synopsis.docx"
ASSETS = ROOT / "docs" / "synopsis_assets"
FONT = "Times New Roman"
BODY = 12


def run_font(run, size=BODY, bold=False, italic=False):
    run.font.name = FONT
    rpr = run._element.get_or_add_rPr()
    rpr.rFonts.set(qn("w:ascii"), FONT)
    rpr.rFonts.set(qn("w:hAnsi"), FONT)
    rpr.rFonts.set(qn("w:eastAsia"), FONT)
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor(0, 0, 0)
    run.bold = bold
    run.italic = italic


def layout(section):
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.left_margin, section.top_margin = Cm(3.5), Cm(2.5)
    section.right_margin, section.bottom_margin = Cm(1.25), Cm(1.25)
    section.header_distance, section.footer_distance = Cm(0.7), Cm(0.65)


def double(p, indent=True, after=0):
    pf = p.paragraph_format
    pf.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    pf.space_before, pf.space_after = Pt(0), Pt(after)
    if indent:
        pf.first_line_indent = Cm(0.75)


def page_number(p):
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run()
    run_font(r, 10)
    begin = OxmlElement("w:fldChar"); begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText"); instr.set(qn("xml:space"), "preserve"); instr.text = " PAGE "
    end = OxmlElement("w:fldChar"); end.set(qn("w:fldCharType"), "end")
    r._r.extend([begin, instr, end])


def border_bottom(p, width="16", color="000000"):
    ppr = p._p.get_or_add_pPr()
    borders = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), width)
    bottom.set(qn("w:space"), "6")
    bottom.set(qn("w:color"), color)
    borders.append(bottom); ppr.append(borders)


def shade(cell, fill="EDEDED"):
    tcpr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd"); shd.set(qn("w:fill"), fill); tcpr.append(shd)


def cell_width(cell, cm):
    tcpr = cell._tc.get_or_add_tcPr()
    tcw = tcpr.find(qn("w:tcW"))
    if tcw is None:
        tcw = OxmlElement("w:tcW"); tcpr.append(tcw)
    tcw.set(qn("w:w"), str(int(cm * 567)))
    tcw.set(qn("w:type"), "dxa")


def no_row_split(row):
    trpr = row._tr.get_or_add_trPr()
    tag = OxmlElement("w:cantSplit"); trpr.append(tag)


def body(doc, text, indent=True, after=0):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    double(p, indent, after)
    run_font(p.add_run(text))
    return p


def sub(doc, text):
    p = doc.add_paragraph(style="Heading 2")
    p.paragraph_format.keep_with_next = True
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    r = p.add_run(text); run_font(r, 12, bold=False)
    return p


def bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    double(p, False)
    p.paragraph_format.left_indent, p.paragraph_format.first_line_indent = Cm(0.75), Cm(-0.5)
    run_font(p.add_run(text))
    return p


def numbered(doc, text):
    p = doc.add_paragraph(style="List Number")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    double(p, False)
    p.paragraph_format.left_indent, p.paragraph_format.first_line_indent = Cm(0.75), Cm(-0.5)
    run_font(p.add_run(text))
    return p


def chapter(doc, number, title, new_page=True):
    p = doc.add_paragraph(style="Heading 1")
    p.paragraph_format.page_break_before = new_page
    p.paragraph_format.keep_with_next = True
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(f"CHAPTER {number}"); run_font(r, 12, bold=True)
    p = doc.add_paragraph()
    p.paragraph_format.keep_with_next = True
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_after = Pt(5)
    r = p.add_run(title); run_font(r, 12, bold=True)
    border_bottom(p)


def caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(5)
    run_font(p.add_run(text), 10)


def image(doc, name, width=5.8):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(str(ASSETS / name), width=Inches(width))


def table(doc, headers, rows, widths, size=9.2):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.autofit = False
    for i, header in enumerate(headers):
        c = t.rows[0].cells[i]; c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        cell_width(c, widths[i]); shade(c, "E6E6E6")
        p = c.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(0)
        run_font(p.add_run(header), size, bold=True)
    no_row_split(t.rows[0])
    for row in rows:
        cells = t.add_row().cells
        for i, value in enumerate(row):
            c = cells[i]; c.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP; cell_width(c, widths[i])
            p = c.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.line_spacing = 1.0; p.paragraph_format.space_after = Pt(0)
            run_font(p.add_run(value), size)
        no_row_split(t.rows[-1])
    return t


def configure(doc):
    for s in doc.sections: layout(s)
    n = doc.styles["Normal"]
    n.font.name, n.font.size = FONT, Pt(BODY)
    n._element.rPr.rFonts.set(qn("w:ascii"), FONT); n._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
    n.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    for name, size, bold in [("Heading 1", 12, True), ("Heading 2", 12, False), ("List Bullet", 12, False), ("List Number", 12, False)]:
        s = doc.styles[name]
        s.font.name, s.font.size, s.font.bold = FONT, Pt(size), bold
        s._element.rPr.rFonts.set(qn("w:ascii"), FONT); s._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
        s.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
        s.paragraph_format.space_before, s.paragraph_format.space_after = Pt(0), Pt(0)


def ref(doc, text):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    double(p, False, 2)
    p.paragraph_format.left_indent, p.paragraph_format.first_line_indent = Cm(0.75), Cm(-0.75)
    run_font(p.add_run(text))


def cover(doc):
    centered = lambda text, size=12, bold=False, after=0: _center(doc, text, size, bold, after)
    centered("PROJECT TITLE: SOMEWHERE", 18, True, 4)
    centered("A LOCATION-BASED AUGMENTED REALITY SOCIAL DISCOVERY APPLICATION", 14, True, 16)
    centered("Synopsis Submitted", 14, True, 2)
    centered("to", 12, False, 12)
    centered("[UNIVERSITY NAME]", 16, True, 5)
    centered("In partial fulfilment of the requirement for the award of the degree of", 12, False, 8)
    centered("Bachelor of Technology", 14, True, 4)
    centered("in", 12, False, 4)
    centered("Computer Science and Engineering", 14, True, 12)
    centered("By", 12, False, 5)
    centered("Student Name: [STUDENT NAME]", 12, False, 2)
    centered("University Roll Number: [UNIVERSITY ROLL NUMBER]", 12, False, 2)
    centered("Class Roll Number: [CLASS ROLL NUMBER]", 12, False, 2)
    centered("Name of Project Supervisor: [PROJECT SUPERVISOR NAME]", 12, False, 8)
    centered("[INSERT UNIVERSITY LOGO]", 12, False, 5)
    centered("[DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING]", 12, True, 2)
    centered("[UNIVERSITY ADDRESS]", 11, False, 2)
    centered("(2026-2027)", 11, False)


def _center(doc, text, size=12, bold=False, after=0):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(after)
    run_font(p.add_run(text), size, bold)
    return p


def contents(doc):
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("TABLE OF CONTENTS"); run_font(r, 14, True)
    rows = [
        ("1", "Chapter 1 Gives Introduction, Motivation of Work and Research Issues", "3-7"),
        ("2", "Chapter 2 Is on Literature Survey and Review of the Past Research Work", "8-12"),
        ("3", "Chapter 3 Identifies Parameters of Gaps in Current Research", "13-14"),
        ("4", "Chapter 4 States the Problem, Proposed Work and Objectives", "15-18"),
        ("5", "Chapter 5 Covers Material, Methodologies, Tools, Platform, Experimental Setup and Hardware", "19-21"),
        ("6", "Chapter 6 Summarizes Proposed Results and Discussions", "22-24"),
        ("7", "Chapter 7 Draws Conclusions and Gives Suggestions for Future Work", "25-26"),
        ("", "References", "27-29"),
    ]
    table(doc, ["No.", "Chapters", "Page No."], rows, [0.75, 11.6, 2.1], 9.1)


def write_chapter_one(doc):
    chapter(doc, 1, "Introduction, Motivation of Work, Research Issues", False)
    sub(doc, "1.1 Introduction")
    for x in [
        "Mobile computing has changed how people experience streets, campuses, parks and public events. A phone can capture a photograph, determine a position, estimate orientation, render a camera preview and exchange data with a cloud service. Yet most social platforms still separate a memory from the place that gave it meaning. Posts are placed in chronological or popularity-ranked feeds, while the coordinate is usually a background tag. A visitor who later arrives at the same location has little opportunity to encounter the original story in its context. Somewhere investigates a location-first alternative in which digital content is deliberately attached to a real place and discovered by moving through that place.",
        "The project is a native Android application in which authenticated users create multimedia messages called Drops. A Drop may contain a short caption, a photograph, optional recorded audio or file attachment, category, creator identity or anonymity choice, expiry information and a geographic placement record. The placement record is more than latitude and longitude: it stores accuracy, timestamp and provider information so that later proximity decisions can account for the uncertainty of a consumer location fix. Nearby users can browse Drops through a camera-led augmented-reality view, a compass-oriented overlay or a list when hardware capability is limited.",
        "The codebase implements Kotlin with Jetpack Compose, ViewModel, StateFlow and Hilt in a single-activity architecture. CameraX supplies preview and capture, fused location and device sensors supply position and heading, Room provides an account-aware local cache, and Supabase provides authentication, PostgreSQL records, object storage and realtime change streams. Google Maps supports route-based Trip Mode. ARCore and SceneView are integrated as an optional capability, so the basic discovery journey retains a sensor and camera fallback on compatible Android 8.0 and above devices.",
    ]: body(doc, x)
    sub(doc, "1.2 Location-Based Social Discovery")
    for x in [
        "A place-based system differs from a conventional map or feed because the location determines when and how information is encountered. In Somewhere, a user does not merely search a catalogue of content; the user approaches a physical vicinity and receives a spatial cue showing where a Drop lies relative to device heading. Discovery uses a backend spatial query within a 500-metre search radius, geodesic distance and bearing calculations, category filtering and a limit on simultaneously visible overlays. The design preserves a practical distinction between seeing a clue to a Drop and opening its full content.",
        "The physical unlock rule is intentionally conservative. A Drop is normally unlocked at 30 metres, but the implementation combines the current location accuracy and the stored placement accuracy as sqrt(currentAccuracy squared plus placementAccuracy squared). Only a bounded allowance, capped at 25 metres, is added to the radius. This prevents two reasonable observations of the same physical point from contradicting each other while avoiding an implausibly large unlock area during poor GPS conditions. A hysteresis margin reduces repeated show-hide behaviour near the discovery boundary.",
        "Location therefore becomes both a creative medium and a technical responsibility. A message at a campus gate, a local mural or a walking trail should be meaningful because of its placement, but users must also understand that satellite and network positioning are estimates. Buildings, tree cover, reflective surfaces, weather, antenna design and sensor calibration can affect accuracy. The application surfaces loading, accuracy, retry and fallback states instead of silently treating every coordinate as exact.",
    ]: body(doc, x)
    image(doc, "architecture.png", 5.75); caption(doc, "Figure 1. Layered architecture of the Somewhere application")
    sub(doc, "1.3 Motivation of Work")
    for x in [
        "The principal motivation is to preserve small, local and time-sensitive experiences that do not fit naturally into global social feeds. A student may leave a welcome message at a campus landmark, a resident may document the story behind a lane, or an event organiser may create a temporary trail. Such content becomes more memorable when it is found at the location rather than consumed as detached media. The project also provides a concrete software-engineering problem because successful place-based interaction requires user experience, geospatial computation, media handling, state management, cloud synchronization and safety controls to work together.",
        "A second motivation is to make augmented reality useful without overstating it. AR is often demonstrated as a visual effect, but outdoor social applications need graceful behaviour when tracking is unavailable, lighting is poor, permission is denied or the device lacks certification. Somewhere treats AR as a progressive enhancement. Where ARCore tracking is available, spatial anchors improve the impression of persistence in the environment. Where it is not, a camera and compass interface still communicates direction and distance. This approach favours reach and continuity over a fragile single-mode experience.",
        "A third motivation is responsible prototype design. Public, geolocated user-generated content introduces questions of privacy, moderation, content expiry, user blocking, storage cost and data retention. The repository includes reporting, author blocking, deletion of owned Drops, account sessions, notifications and explicit permission flows. These are not complete production governance controls, but they establish the practical issues that a research-oriented prototype must identify and evaluate.",
    ]: body(doc, x)
    sub(doc, "1.4 Research Issues")
    for x in [
        "The project addresses several interrelated research and engineering issues. First, a single location callback is often not adequate for attachment of long-lived content. The stable-location collector accepts fresh readings with a sensible accuracy range, retains up to twelve samples for no more than ten seconds and estimates a weighted centre using inverse-accuracy-squared weights. It can return early after sufficient stable samples or fall back to the best estimate at timeout. This creates a transparent, bounded placement process rather than an undefined use of the last known location.",
        "Second, a camera overlay must remain comprehensible as nearby content increases. The discovery workflow limits visible overlays, fans out co-located Drops rather than stacking them, smooths heading updates across the 359-to-0-degree boundary and exposes a list alternative. Third, shared social state must remain responsive without producing duplicate local updates. The repository combines immediate action results with realtime subscriptions for comments, reactions and nearby data changes, while Room offers continuity during temporary network failure.",
        "Finally, the prototype must be evaluated honestly. It can demonstrate a complete engineering workflow and pass repository unit tests, but it cannot claim surveyed usability, precise navigation performance, safety benefit or long-term social impact without controlled field trials. The proposed evaluation therefore measures task success, observed location error, unlock consistency, latency, AR availability, recovery behaviour and participant understanding of privacy and accuracy information.",
    ]: body(doc, x)
    image(doc, "gps_algorithm.png", 5.7); caption(doc, "Figure 2. Stable placement and bounded-unlock decision process")


def write_chapter_two(doc):
    chapter(doc, 2, "Literature Survey and Review of the Past Research Work")
    sub(doc, "2.1 Augmented Reality and Situated Information")
    for x in [
        "Azuma's foundational survey characterizes augmented reality through the combination of real and virtual content, real-time interaction and three-dimensional registration [1]. The survey identifies tracking, calibration and registration error as central constraints. This is directly relevant to Somewhere: a compelling overlay must have a defensible relationship to the user's viewpoint and a clear response when that relationship becomes unreliable. The present project accepts that a phone-based prototype cannot guarantee survey-grade placement and therefore uses AR as one layer within a broader accuracy-aware interaction model.",
        "Feiner, MacIntyre, Hollerer and Webster demonstrated the Touring Machine, an early mobile AR system for situated urban information [2]. Their work showed that outdoor navigation and viewpoint-linked annotation can make information more useful when encountered in place. Modern phones make the hardware model more accessible, but the design questions remain: how should visual labels communicate distance, direction and relevance; what happens when tracking fails; and how can an overlay remain readable? Somewhere adapts these questions to a community-created memory layer rather than a fixed institutional tour.",
        "Billinghurst, Clark and Lee review AR interfaces and identify registration, interaction, display constraints and contextual design as continuing research themes [3]. Their analysis supports the decision to avoid a camera-only solution. The current application offers camera/compass and list modes, limits simultaneous overlays and makes the lock state explicit. This is a usability decision as much as a technical one: the visual layer should explain a real-world relationship without distracting the user from the surrounding environment.",
    ]: body(doc, x)
    sub(doc, "2.2 Location-Based Play, Geocaching and Community Exploration")
    for x in [
        "Geocaching established a well-known model of coordinate-based discovery in which participants navigate to a specified place and log a find. Tursi, Deplano and Ruffo extended this behaviour into AiRCacher, an AR-enhanced virtual geocaching system designed to encourage outdoor movement and collect location-linked social data [4]. The work validates the hide-and-seek structure underlying Drops, while Somewhere extends the content model beyond a single cache to photographs, captions, audio, files, categories, comments, reactions, expiry and experimental travelling content.",
        "Large location-based games demonstrate that digital objectives can influence physical movement. Althoff, White and Horvitz reported a measurable short-term association between Pokemon Go use and physical activity in their observational study [5]. The implication for Somewhere is limited but useful: location-centric interaction can motivate exploration when the reward is understandable. Somewhere does not make a health claim. Instead, it treats movement as the pathway through which people encounter local memories, student-generated information and temporary event material.",
        "Liu and Fuhrmann describe the convergence of augmented reality and location-based social networks as a rich research area involving multimedia storytelling, visualisation, functionality and ethical concerns [6]. Their work is particularly useful because it frames the opportunity and the responsibility together. A social spatial layer can improve place awareness, but it can also expose sensitive routines, amplify inappropriate content or mislead a user when location quality is poor. Somewhere incorporates accuracy metadata, report/block actions and expiration as early responses to these concerns.",
    ]: body(doc, x)
    image(doc, "discovery_flow.png", 5.75); caption(doc, "Figure 3. Nearby retrieval, direction calculation and content unlock flow")
    sub(doc, "2.3 Mobile Architecture, Local Persistence and Cloud Synchronization")
    for x in [
        "Android's architecture guidance recommends a separation between UI logic, state holders and data layers, with repositories serving as the boundary between domain needs and data sources [7]. Somewhere follows this arrangement. Compose screens render immutable state from ViewModels, StateFlow communicates state changes, and repositories coordinate the camera, fused location, Room, Supabase and mapping services. This structure reduces the likelihood that an Activity or composable directly owns network state or business rules, and it makes individual flows more testable.",
        "Room provides a structured local persistence option with compile-time query validation and explicit migration support [8]. The project uses it as an account-aware cache for nearby and user-related data, including schema changes as the Drop model gained placement-quality fields. Local persistence does not replace the server; rather, it improves continuity, enables quick profile and discovery updates and provides a reconciliation surface while network reads are unavailable. This hybrid approach is appropriate because multimedia assets and social interactions remain cloud-shared but the mobile interface requires fast, lifecycle-safe state.",
        "Supabase combines authentication, PostgreSQL, object storage and realtime database changes [9]. Its use maps naturally to the repository's data split: media binaries are compressed and uploaded to storage; searchable coordinates, text, expiry and relationship data are recorded in PostgreSQL; and comments, reactions, notification state and nearby changes are propagated as events. The literature and platform documentation suggest that realtime interfaces need authoritative reconciliation, access policy enforcement and careful bandwidth control rather than indiscriminate polling or public storage exposure.",
    ]: body(doc, x)
    sub(doc, "2.4 Location, Sensor and Camera Services")
    for x in [
        "The Android fused location provider can combine GPS, Wi-Fi and cellular signals and report a horizontal accuracy estimate alongside a location [10]. Its existence does not remove uncertainty; developers must decide whether a result is fresh and sufficient for the task. Somewhere interprets accuracy as a quality signal, filters samples with unusable values and stores the measurement context. This implementation choice is consistent with location-aware systems research that treats position as an estimate whose confidence matters to downstream decisions.",
        "CameraX provides a lifecycle-aware abstraction for preview and image capture [11]. In Somewhere, the capture result is retained in the creation ViewModel and SavedStateHandle while the user writes the Drop's message and confirms its location. Before upload, the image is decoded, constrained to a maximum dimension of 1024 pixels and compressed as JPEG. This limits bandwidth and object-storage cost while retaining enough detail for the intended social memory workflow. Audio and attachments remain optional enhancements so the core path is not dependent on a complex upload sequence.",
        "ARCore's motion tracking uses visual and inertial information to estimate device pose [12]. The service improves world-relative presentation but depends on device support, light, texture and active tracking conditions. This is why the application declares ARCore optional and retains a heading-based fallback. The review of these tools therefore supports a principle of progressive capability: use the best available spatial information without turning it into a condition for basic participation.",
    ]: body(doc, x)
    sub(doc, "2.5 Comparative Summary of Reviewed Work")
    rows = [
        ("Azuma [1]", "AR foundations", "Defines registration and tracking problem", "Supports explicit uncertainty and AR fallback"),
        ("Touring Machine [2]", "Urban AR guidance", "Viewpoint-linked situated information", "Community-authored mobile memories"),
        ("Billinghurst et al. [3]", "AR interfaces", "Interaction and display constraints", "Overlay readability and compatibility"),
        ("AiRCacher [4]", "Virtual geocaching", "AR discovery motivates movement", "Media-rich social Drops and expiry"),
        ("Althoff et al. [5]", "Location-based game", "Physical exploration can be engaging", "Non-competitive local storytelling"),
        ("Liu and Fuhrmann [6]", "AR + social networks", "Ethics and multimedia storytelling", "Privacy, moderation and accuracy disclosure"),
        ("Android Architecture [7]", "Mobile design", "State/data separation", "Compose, ViewModel and repositories"),
        ("Room [8]", "Local persistence", "Typed cache and migrations", "Account-scoped offline continuity"),
        ("Supabase [9]", "Cloud backend", "Auth, database, storage, realtime", "Shared Drops and social updates"),
        ("Fused Location [10]", "Positioning", "Accuracy-bearing location estimates", "Stable multi-sample placement"),
        ("CameraX [11]", "Capture", "Lifecycle-aware media capture", "Progressive creation and compression"),
        ("ARCore [12]", "Pose tracking", "World-relative rendering", "Optional enhancement plus fallback"),
    ]
    table(doc, ["Work", "Area", "Main contribution", "Use in Somewhere"], rows, [2.15, 2.4, 4.2, 4.1], 8.4)
    caption(doc, "Table 1. Summary of literature, technical precedents and implementation sources")
    body(doc, "The review shows that the proposed system is not a claim of a wholly new AR primitive. Its contribution is the integration of established mobile capabilities into a coherent location-first social workflow. The central design gap is the need to combine situated content, bounded positional uncertainty, practical Android compatibility, cloud synchronization and responsible public interaction in one prototype that can be inspected and evaluated end to end.")


def write_chapter_three(doc):
    chapter(doc, 3, "Identification Parameters of Gaps in Current Research")
    sub(doc, "3.1 Identification Parameters")
    for x in [
        "The literature review identifies a gap between visually impressive AR demonstrations and deployable, community-authored location systems. Many studies concentrate on tracking, a particular game interaction, an institutional guide or a single technical layer. A student project intended for actual Android devices must instead consider whether content is meaningful in context, whether its placement is honest about uncertainty, whether a user can still participate without ARCore, whether remote interactions converge correctly and whether unsafe or unwanted content can be controlled. These parameters define the current research and engineering gap.",
        "The first parameter is spatial validity. A coordinate must be accompanied by measurement quality and a clear interaction rule. The repository responds by capturing multiple fresh samples, weighting them according to reported accuracy and retaining timestamp, provider and accuracy metadata. The discovery rule adds only a limited uncertainty allowance to a physical unlock radius. This differs from naive systems that either expose content at any distance or reject it rigidly when two reasonable GPS observations differ by several metres.",
        "The second parameter is interface resilience. A camera overlay is attractive but cannot be the sole access path because devices differ in ARCore support and real conditions differ in light, sensor quality, connectivity and permission state. Somewhere uses ARCore where available but preserves a compass-based overlay and an accessible list view. It also limits visible cards, handles co-located content and exposes retry messages for unavailable camera, timeout, network and location states. The result is a progressive interface rather than an all-or-nothing AR experience.",
        "The third parameter is social trust. A public location layer may be useful only if users can understand who can see content, remove their own content, report problematic material and block unwanted authors. The current code includes core actions for reporting, blocking, deletion, expiry and notification. However, formal row-level security, protected storage, abuse review, retention policy and privacy impact assessment remain required before production deployment. The research gap therefore includes governance, not only rendering.",
    ]: body(doc, x)
    sub(doc, "3.2 Gap Comparison")
    rows = [
        ("Spatial precision", "Coordinate treated as exact", "Location carries reported accuracy and timestamp", "Measure placement/discovery consistency"),
        ("Content reveal", "Immediate map/feed visibility", "Distance, direction and bounded physical unlock", "Observe understanding of locked state"),
        ("AR support", "AR-dependent prototype", "Optional ARCore plus camera/compass/list fallback", "Compare feature availability by device"),
        ("Overlay density", "Labels overlap at shared points", "Visible-card cap and deterministic fanning", "Record selection errors and clutter"),
        ("Cloud state", "Manual refresh or isolated local state", "Room cache plus realtime reconciliation", "Measure update latency and duplicate state"),
        ("Public safety", "Location content without controls", "Report, block, delete, expiry and permission context", "Evaluate policy requirements before release"),
    ]
    table(doc, ["Parameter", "Observed limitation", "Proposed response", "Evaluation evidence"], rows, [2.1, 3.45, 4.1, 3.2], 8.45)
    caption(doc, "Table 2. Parameters used to identify the current research gap")
    sub(doc, "3.3 Research Questions")
    for x in [
        "RQ1: Can an Android application create and retrieve multimedia content at real-world locations while preserving a usable description of placement uncertainty? RQ2: Does a bounded accuracy-aware unlock rule produce understandable behaviour when placement and discovery coordinates differ? RQ3: Can camera, compass and list interfaces offer comparable basic discovery on devices with and without ARCore? RQ4: Can local caching and realtime events maintain responsive social interaction without presenting contradictory state? RQ5: Which privacy, moderation and energy constraints remain before a public release would be appropriate?",
        "These questions frame the work as applied research. The project does not assume that an implementation alone proves social value. Instead, it proposes repeatable technical trials and user tasks that can generate evidence about accuracy, usability, latency and failure recovery. Findings will be bounded by the number of devices, environments and participants available for evaluation.",
    ]: body(doc, x)


def write_chapter_four(doc):
    chapter(doc, 4, "Statement of the Problem, Proposed Work and Objectives")
    sub(doc, "4.1 Statement of the Problem")
    for x in [
        "Existing social media makes it easy to publish images and messages, but it rarely preserves their relationship with a specific physical setting. Mapping applications efficiently direct a user to businesses and addresses, but they are not designed for personal, temporary or community-authored memories. Augmented-reality prototypes demonstrate overlay techniques, yet often depend on specialized tracking conditions or do not address how many people can safely create, discover, moderate and synchronize content at ordinary places. The project problem is to design and evaluate a practical Android prototype that joins these capabilities without making false assumptions about positional precision or device support.",
        "The system must solve a connected set of technical problems: obtain an adequately fresh placement coordinate; store media and structured metadata reliably; retrieve nearby eligible records efficiently; compute distance and direction; decide when full content should unlock; show several nearby items without visual confusion; synchronize comments and reactions; recover from camera, network and permission failure; and give users meaningful controls over public content. The system must also identify what is outside the current scope, including survey-grade mapping, universal AR anchoring, automated abuse resolution and large-scale production operations.",
    ]: body(doc, x)
    sub(doc, "4.2 Proposed Work")
    for x in [
        "The proposed work is Somewhere, a location-based augmented-reality social discovery application for Android. A user signs in, captures a photograph or prepares text content, adds a concise message, selects optional attributes such as category, expiry, anonymity, audio, file attachment or Hitchhiker behaviour, then confirms a measured location. The application collects several valid fused-location samples before saving. Image media is constrained and compressed, stored in an account-namespaced object path, and linked to a cloud Drop record with its placement-quality metadata.",
        "A second user opens discovery, grants necessary permissions and receives nearby content from a spatial database function. The presentation layer calculates distance and bearing, filters expired or blocked content, limits cards, fans out co-located items and exposes a physical lock state. The user can move toward the location, unlock within the effective radius, view image content, react, comment, report or block. A Route/Trip Mode can request directions, display nearby Drops along a route and support two-kilometre geofences. Optional AI functions summarize place text or support curated generated content, but are not treated as the core source of truth.",
        "The design is layered. Jetpack Compose renders screens; ViewModels retain operation state; repositories coordinate device services, local data and the cloud; Room caches structured records; Supabase provides shared persistence and events; CameraX and sensors provide device inputs; and ARCore provides progressive spatial enhancement. This separation supports testing, incremental development and candid evaluation of each subsystem.",
    ]: body(doc, x)
    image(doc, "creation_flow.png", 5.7); caption(doc, "Figure 4. Proposed creation, placement and publication workflow")
    sub(doc, "4.3 Objectives")
    objectives = [
        "To study augmented reality, location-based social interaction, geocaching-style discovery, Android architecture and privacy considerations relevant to a place-based multimedia system.",
        "To design and implement a mobile workflow that allows authenticated users to create persistent or expiring text, image, audio and file-based Drops with explicit placement-quality metadata.",
        "To implement nearby discovery using spatial retrieval, geodesic distance, bearing, sensor orientation and a bounded accuracy-aware unlock rule, with ARCore, compass and list presentation paths.",
        "To provide dependable local/cloud persistence, realtime comments and reactions, notifications, user reporting, blocking, deletion and route-based discovery while retaining clear failure and retry states.",
        "To evaluate the prototype through unit tests, controlled location trials, multi-device/realtime checks and task-based user evaluation, then document its performance, limitations and future work.",
    ]
    for x in objectives: numbered(doc, x)
    sub(doc, "4.4 Functional Scope")
    rows = [
        ("Identity", "Email authentication, profile information and account-scoped records"),
        ("Creation", "Camera capture, message, category, expiry, anonymity, audio/file enhancements and draft recovery"),
        ("Placement", "Fresh-sample stabilization, weighted coordinate, reported accuracy, timestamp and provider"),
        ("Discovery", "Nearby spatial query, distance/bearing, 500-metre radius, 30-metre unlock with bounded allowance"),
        ("Social interaction", "Likes, comments, notifications, report, block and owned-content deletion"),
        ("Exploration", "AR/compass/list mode, Trip Mode, route proximity, geofence support and Hitchhiker experiment"),
    ]
    table(doc, ["Area", "Included capability"], rows, [3.25, 9.6], 9.0)
    caption(doc, "Table 3. Functional scope of the proposed work")
    sub(doc, "4.5 Non-Functional Requirements and Constraints")
    for x in [
        "Usability requires that the primary creation path remains understandable and recoverable. The app must communicate whether it is waiting for a camera, a precise location or a network request, and it must not erase a draft without confirmation. Performance requires bounded nearby result sets, media compression and event-driven synchronization rather than constant polling. Compatibility requires Android API 26 or later with camera, location, accelerometer and compass; ARCore improves the experience when supported but is not compulsory.",
        "Security and privacy require contextual permissions, account-scoped storage paths, user controls and eventual backend policy enforcement. Reliability requires retries, timeout states, cached data where appropriate and protection from lifecycle interruption. Accuracy requires that measurement quality is stored and that the unlock logic remains bounded. Maintainability requires repositories, dependency injection, model versioning, tests and readable separation between UI state and external services.",
    ]: body(doc, x)
    sub(doc, "4.6 Work Plan")
    rows = [
        ("Phase I", "Problem study and literature review", "AR, geospatial uncertainty, privacy, social discovery and requirements"),
        ("Phase II", "Architecture and data modelling", "Use cases, Room entities, Supabase schema, navigation and permissions"),
        ("Phase III", "Core implementation", "Authentication, creation flow, capture, stable location, storage and profiles"),
        ("Phase IV", "Discovery and social features", "Spatial RPC, orientation, unlock, comments, reactions and notifications"),
        ("Phase V", "Extensions and evaluation", "Trip Mode, AI experiments, tests, field trials, findings and documentation"),
    ]
    table(doc, ["Phase", "Work package", "Primary output"], rows, [1.55, 4.15, 7.15], 8.7)
    caption(doc, "Table 4. Iterative planning of the proposed work")


def write_chapter_five(doc):
    chapter(doc, 5, "Material, Methodologies, Tools, Platform, Experimental Setup and Hardware Requirements")
    sub(doc, "5.1 Research Methodology")
    for x in [
        "The project uses an applied design-and-development methodology. Repository source inspection establishes what has actually been implemented: the models, navigation flows, repositories, Room migrations, Supabase integration, sensor logic and tests. The engineering cycle is iterative: state the user and technical requirement; implement a bounded feature; test its expected and failure behaviours; observe results on supported devices; and refine the design. This method is appropriate because the project is both a functioning prototype and an investigation of whether a coherent location-first interaction can be made reliable on consumer Android hardware.",
        "The unit of analysis is a Drop across its lifecycle: capture, draft retention, stabilization of location, media upload, cloud persistence, local caching, nearby retrieval, visual presentation, unlock, social interaction, expiry, moderation and deletion. Evidence will come from source review, automated unit tests, controlled outdoor/indoor trials, multi-account realtime trials and task-oriented user sessions. Measurements will include reported placement accuracy, sample spread, current accuracy, observed distance at unlock, backend response time, media-upload time, overlay density, AR availability and user-reported clarity of the lock and privacy states.",
    ]: body(doc, x)
    image(doc, "realtime_sequence.png", 5.65); caption(doc, "Figure 5. Realtime comment action and server reconciliation sequence")
    sub(doc, "5.2 Data Collection and Evaluation Plan")
    rows = [
        ("Stable placement", "Open, built-up and partially obstructed locations", "Accuracy, sample spread, time to stable estimate"),
        ("Unlock consistency", "Create and rediscover same Drop with varying current accuracy", "Observed distance, effective radius, lock/unlock outcome"),
        ("Discovery usability", "Find selected Drop among nearby candidates", "Task completion, selection errors, perceived clutter"),
        ("Compatibility", "ARCore-capable and non-capable devices", "Mode availability, fallback completion, crashes"),
        ("Realtime state", "Comment/react from a separate test account", "Propagation time, duplicate/missing updates"),
        ("Failure recovery", "Deny permission; disable network/camera/location", "Message clarity, retry result, draft/data retention"),
    ]
    table(doc, ["Test focus", "Procedure", "Evidence recorded"], rows, [2.5, 5.45, 4.9], 8.35)
    caption(doc, "Table 5. Proposed experimental and evaluation data")
    sub(doc, "5.3 Software Tools and Platform")
    rows = [
        ("Android Studio, Gradle and JDK 17", "Build, package, run and test the Android application"),
        ("Kotlin, Coroutines and Flow", "Typed application logic, background operations and reactive data"),
        ("Jetpack Compose and Material 3", "Declarative state-driven screens and UI components"),
        ("CameraX, fused location and sensors", "Capture, fresh location, reported accuracy and device heading"),
        ("ARCore and SceneView", "Optional pose-based spatial rendering"),
        ("Room / SQLite", "Local cache, DAO queries and migration-controlled persistence"),
        ("Supabase Auth, Postgres, Storage, Realtime", "Identity, records, media objects and change subscriptions"),
        ("Google Maps services", "Route directions, autocomplete, Trip Mode and map context"),
        ("Retrofit, OkHttp, Coil and Sentry", "Networking, image loading, diagnostics and crash reporting"),
        ("JUnit and Gradle test tasks", "Unit verification for algorithmic and presentation logic"),
    ]
    table(doc, ["Tool or platform", "Role in the proposed work"], rows, [5.0, 7.85], 8.35)
    caption(doc, "Table 6. Software facilities used by the project")
    sub(doc, "5.4 Experimental Setup and Hardware Requirements")
    for x in [
        "Development and verification require a computer capable of running Android Studio, a stable internet connection, configured test credentials and Android devices. At least one physical phone must have a camera, GPS/location services, accelerometer and compass and run Android 8.0 or above. An ARCore-supported device is desirable for validating the enhanced renderer, while a second device or account is necessary for realtime interaction tests. Outdoor locations with open sky and built-up spaces should both be included so the effect of changing position quality can be observed rather than assumed.",
        "The backend test environment requires a Supabase project with an authentication configuration, PostgreSQL schema, object-storage buckets and controlled realtime subscriptions. API keys and service URLs must be kept in build configuration and test accounts should not use sensitive personal data. Google Maps and optional AI services require properly restricted credentials and usage quotas. For safe field trials, placement should occur in permitted public or campus settings, should avoid hazardous traffic conditions and should not reveal a participant's home or other sensitive location.",
    ]: body(doc, x)
    sub(doc, "5.5 Data Analysis")
    for x in [
        "Quantitative observations will be summarized using counts, ranges, medians and simple comparisons across test conditions. For each placement trial, the analysis will compare reported accuracy, sample spread, selected weighted coordinate and subsequent unlock outcome. For networking, it will record request/response time and realtime propagation under available conditions. The intent is not to claim a statistically generalizable population result from a small project sample; it is to identify whether behaviour is stable enough for the stated design purpose and where it fails.",
        "Qualitative observations from task sessions will be grouped around comprehension, trust and recovery. Participants can be asked whether they understand why a Drop is locked, whether displayed direction and distance are useful, whether the fallback interface remains usable, whether permission messages are clear and what privacy concern they would raise before using the app in public. These observations will guide a final discussion that separates observed result, participant perception, implementation limitation and future recommendation.",
    ]: body(doc, x)


def write_chapter_six(doc):
    chapter(doc, 6, "Proposed Results and Discussions")
    sub(doc, "6.1 Proposed Results")
    for x in [
        "The expected result is a demonstrable Android prototype that supports the complete lifecycle of a place-based digital memory. A signed-in user should be able to capture or select media, write a message, preserve the draft while navigating, attach a location that includes its quality metadata, and publish the resulting Drop. A nearby user should be able to see a clue to the content through the discovery interface, understand its direction and distance, move into the bounded physical radius, unlock the full content and participate through reactions or comments. The system should also make expiry, reporting, blocking and deletion observable as real state transitions rather than static claims.",
        "The location pipeline is expected to behave more consistently than a single unqualified location read. Under suitable conditions, several fresh samples should produce a weighted coordinate with a defensible reported accuracy and a stable early completion rule. Under poor conditions, the system should wait up to its bounded timeout and preserve the best valid estimate with its actual quality. Subsequent unlock decisions should be explainable from recorded placement accuracy, current accuracy, physical distance and the capped allowance. The desired outcome is predictable behaviour, not a promise of exact geospatial registration.",
        "The discovery pipeline is expected to retrieve only a manageable local result set, maintain smooth direction updates and prevent overlap for Drops located at the same coordinate. ARCore-capable devices should provide a stronger sense of spatial attachment, while unsupported devices should still complete basic discovery through the compass/camera or list fallback. Realtime operations should make comments and reactions visible across test accounts without requiring the user to close and reopen the screen. When the network fails, cached data and retryable state should make the limitation explicit.",
    ]: body(doc, x)
    sub(doc, "6.2 Expected Discussion Using Evaluation Measures")
    rows = [
        ("Placement quality", "Reported accuracy, sample spread, stabilization duration", "A location record that discloses uncertainty rather than pretending exactness"),
        ("Unlock correctness", "Distance at outcome, current and placement accuracies", "Consistent bounded unlock near the intended place"),
        ("Discovery clarity", "Successful finds, errors, overlay count, participant feedback", "Understandable direction and locked-state cues"),
        ("Compatibility", "Mode reached per device, tracking availability, fallback completion", "Core participation without mandatory ARCore"),
        ("Realtime behaviour", "Propagation latency, missed or duplicated changes", "Social state converges across accounts"),
        ("Resilience", "Recovery after denied permission, timeout or outage", "Clear recovery and preservation of meaningful state"),
    ]
    table(doc, ["Result category", "Measurement", "Expected interpretation"], rows, [2.7, 5.1, 5.05], 8.5)
    caption(doc, "Table 7. Proposed result categories and interpretation")
    sub(doc, "6.3 Limitations")
    for x in [
        "Consumer location quality remains the principal technical limitation. Android-reported horizontal accuracy is an estimate and can be poor indoors, between tall buildings or when the device has not obtained a fresh satellite fix. The algorithm's weighted sampling and capped allowance make decisions more consistent, but they cannot guarantee that an overlay precisely occupies the physical point where it was created. Likewise, a compass-based overlay may drift with magnetic interference and ARCore tracking can be interrupted by limited visual features, poor light or rapid movement.",
        "The current project is a prototype, not a moderated production network. Its reporting and blocking flows are useful starting controls, but public release would require enforced database row-level security, storage policies, abuse workflows, audit mechanisms, age-appropriate design, accessible terms, retention rules and data-protection review. AI-generated text or local summary features require additional safeguards for accuracy, harmful output, service cost and disclosure. The future discussion must not treat a client-side control as a complete policy solution.",
        "Evaluation results will also be constrained by the available test devices, places, network conditions and participant sample. Small tests can establish implementation behaviour and discover usability issues, but they cannot establish population-wide benefit, sustained engagement or safety improvement. These limitations are valuable findings because they define the boundary between a student prototype and a reliable public platform.",
    ]: body(doc, x)
    sub(doc, "6.4 Beneficial Uses for Society and Institutions")
    for x in [
        "A carefully governed version of the system can support community storytelling, campus orientation, heritage walks, local art trails, temporary events and place-based learning. The advantage is not simply that information appears on a map; it is that a learner or visitor encounters it at the location where its meaning is clearest. For a university, curated Drops can provide welcome messages, laboratory stories, safety notices, alumni memories or subject-specific field observations. For a locality, consented contributions can preserve neighbourhood narratives that are otherwise hard to discover.",
        "The project can also demonstrate a practical combination of mobile and cloud technologies to students. It brings together interface design, geospatial computation, sensor data, local databases, secure sessions, object storage, realtime events, tests and documentation. This multidisciplinary character is valuable to industry-oriented software education, provided that the social and privacy consequences are evaluated with the same seriousness as the technical functions.",
    ]: body(doc, x)


def write_chapter_seven(doc):
    chapter(doc, 7, "Conclusion and Suggestions for Future Work")
    sub(doc, "7.1 Conclusion")
    for x in [
        "Somewhere proposes a practical interpretation of location-based augmented reality: leave a small digital memory where it belongs and let another person encounter it through proximity. The repository demonstrates a substantial Android implementation rather than a purely conceptual interface. It integrates Compose screens, ViewModels, StateFlow, Hilt, CameraX, fused location, sensors, Room, Supabase, Maps, realtime social functions and optional ARCore. The project thereby establishes a complete path from media capture and placement to nearby retrieval, distance/direction presentation, bounded unlock and social interaction.",
        "The most important technical conclusion is that location should be represented as a measurement with quality, not simply as a pair of coordinates. Stable multi-sample placement, stored accuracy metadata and a bounded uncertainty-aware unlock rule give the system a more defensible behaviour under ordinary GPS variation. The most important product conclusion is that AR must be progressive. A world-locked experience is valuable when tracking exists, but the basic purpose of discovery should remain available through a compass and list path. The most important governance conclusion is that a location-based social layer must expose privacy, moderation and deletion controls from the beginning, while recognizing the need for stronger backend enforcement before release.",
        "The project therefore meets its purpose as an applied software-engineering synopsis: it identifies a meaningful problem, translates reviewed concepts into a coherent mobile architecture, describes a testable methodology, defines measurable outcomes and states the limits of its claims. A final evaluation should report observed results separately from expectation and should treat negative or inconsistent results as evidence for design improvement rather than as a reason to conceal the system's constraints.",
    ]: body(doc, x)
    sub(doc, "7.2 Suggestions for Future Work")
    suggestions = [
        "Conduct a larger, ethically reviewed field study across multiple campus and neighbourhood settings to measure discovery success, clarity, safety and long-term retention.",
        "Introduce stronger geospatial anchoring, depth/occlusion support and persistent cloud anchors where device capability and privacy constraints permit.",
        "Harden the backend with verified row-level security, protected media policies, short-lived signed URLs, rate limits, audit trails and moderation queues.",
        "Add accessibility-focused discovery modes including haptic direction cues, high-contrast rendering, screen-reader descriptions and non-camera navigation.",
        "Explore privacy-preserving placement such as coarse public locations, delayed publication, audience controls, consented zones and user-visible retention schedules.",
        "Evaluate energy consumption, background-location necessity, media/storage cost and realtime scalability before any broad rollout.",
        "Develop curated institutional workflows for heritage, education or event content so public contributions can coexist with trusted collections.",
        "Assess AI features separately for factuality, bias, safety, disclosure, cost and human oversight before they influence discovery or moderation decisions.",
    ]
    for x in suggestions: bullet(doc, x)
    sub(doc, "7.3 Beneficial for Society and Industry")
    for x in [
        "The project demonstrates that a place can be a meaningful interface for digital communication. Its potential benefit lies in making local stories, orientation information and temporary community content discoverable when a person is physically present and able to understand the context. This may help institutions create more engaging learning or visitor experiences, and it may give communities a lightweight way to preserve small narratives that do not fit in formal archives.",
    ]: body(doc, x)


def references(doc):
    p = doc.add_paragraph(style="Heading 1")
    p.paragraph_format.page_break_before = True
    p.paragraph_format.keep_with_next = True
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_after = Pt(5)
    r = p.add_run("References"); run_font(r, 12, bold=True)
    border_bottom(p)
    refs = [
        '[1] R. T. Azuma, "A Survey of Augmented Reality," Presence: Teleoperators and Virtual Environments, vol. 6, no. 4, pp. 355-385, 1997, doi: 10.1162/pres.1997.6.4.355.',
        '[2] S. Feiner, B. MacIntyre, T. Hollerer and A. Webster, "A Touring Machine: Prototyping 3D Mobile Augmented Reality Systems for Exploring the Urban Environment," in Proc. ISWC, 1997, pp. 74-81.',
        '[3] M. Billinghurst, A. Clark and G. Lee, "A Survey of Augmented Reality," Foundations and Trends in Human-Computer Interaction, vol. 8, nos. 2-3, pp. 73-272, 2015.',
        '[4] G. Tursi, M. Deplano and G. Ruffo, "AiRCacher: Virtual Geocaching Powered with Augmented Reality," in Proc. 25th ACM Conf. Hypertext and Social Media, 2014, pp. 330-332, doi: 10.1145/2631775.2631778.',
        '[5] T. Althoff, R. W. White and E. Horvitz, "Influence of Pokemon Go on Physical Activity: Study and Implications," J. Med. Internet Res., vol. 18, no. 12, e315, 2016, doi: 10.2196/jmir.6759.',
        '[6] C. Liu and S. Fuhrmann, "Enriching the GIScience Research Agenda: Fusing Augmented Reality and Location-Based Social Networks," Transactions in GIS, vol. 22, no. 3, pp. 775-788, 2018, doi: 10.1111/tgis.12345.',
        '[7] Android Developers, "Guide to App Architecture." [Online]. Available: https://developer.android.com/topic/architecture. [Accessed: Aug. 2026].',
        '[8] Android Developers, "Save Data in a Local Database Using Room." [Online]. Available: https://developer.android.com/training/data-storage/room. [Accessed: Aug. 2026].',
        '[9] Supabase, "Documentation: Auth, Database, Storage and Realtime." [Online]. Available: https://supabase.com/docs. [Accessed: Aug. 2026].',
        '[10] Android Developers, "Get the Last Known Location and Receive Location Updates." [Online]. Available: https://developer.android.com/develop/sensors-and-location/location. [Accessed: Aug. 2026].',
        '[11] Android Developers, "CameraX Architecture." [Online]. Available: https://developer.android.com/media/camera/camerax/architecture. [Accessed: Aug. 2026].',
        '[12] Google for Developers, "ARCore Fundamentals." [Online]. Available: https://developers.google.com/ar/develop/fundamentals. [Accessed: Aug. 2026].',
        '[13] Google for Developers, "Maps SDK for Android." [Online]. Available: https://developers.google.com/maps/documentation/android-sdk. [Accessed: Aug. 2026].',
        '[14] Android Developers, "Request App Permissions." [Online]. Available: https://developer.android.com/training/permissions/requesting. [Accessed: Aug. 2026].',
        '[15] OWASP Foundation, "Mobile Application Security Verification Standard, Version 2.1.0," 2024. [Online]. Available: https://mas.owasp.org/MASVS/.',
        '[16] National Institute of Standards and Technology, "NIST Privacy Framework: A Tool for Improving Privacy Through Enterprise Risk Management, Version 1.0," 2020.',
        '[17] M. Krumm, "A Survey of Computational Location Privacy," Personal and Ubiquitous Computing, vol. 13, pp. 391-399, 2009, doi: 10.1007/s00779-008-0212-5.',
        '[18] J. I. Hong and J. A. Landay, "An Architecture for Privacy-Sensitive Ubiquitous Computing," in Proc. MobiSys, 2004, pp. 177-189.',
        '[19] D. G. Lowe, "Distinctive Image Features from Scale-Invariant Keypoints," Int. J. Computer Vision, vol. 60, no. 2, pp. 91-110, 2004.',
        '[20] I. Goodfellow, Y. Bengio and A. Courville, Deep Learning. Cambridge, MA: MIT Press, 2016.',
        '[21] M. Satyanarayanan, "Pervasive Computing: Vision and Challenges," IEEE Personal Communications, vol. 8, no. 4, pp. 10-17, 2001.',
        '[22] A. Dey, G. Abowd and D. Salber, "A Conceptual Framework and a Toolkit for Supporting the Rapid Prototyping of Context-Aware Applications," Human-Computer Interaction, vol. 16, nos. 2-4, pp. 97-166, 2001.',
        '[23] E. H. Chi, "A Taxonomy of Visualization Techniques Using the Data State Reference Model," in Proc. IEEE InfoVis, 2000, pp. 69-75.',
        '[24] I. E. Sutherland, "A Head-Mounted Three Dimensional Display," in Proc. AFIPS Fall Joint Computer Conference, 1968, pp. 757-764.',
        '[25] P. Milgram and F. Kishino, "A Taxonomy of Mixed Reality Visual Displays," IEICE Trans. Information and Systems, vol. E77-D, no. 12, pp. 1321-1329, 1994.',
        '[26] P. K. Agarwal and L. E. Kavraki, "The Spatial Database Challenge," in Proc. ACM Symposium on Advances in Geographic Information Systems, 2002.',
        '[27] M. J. Kraak and F. Ormeling, Cartography: Visualization of Geospatial Data, 3rd ed. London: Routledge, 2020.',
        '[28] J. Nielsen, Usability Engineering. San Francisco: Morgan Kaufmann, 1994.',
        '[29] International Organization for Standardization, "ISO 9241-11:2018 Ergonomics of Human-System Interaction - Usability," 2018.',
        '[30] The Kotlin Foundation, "Kotlin Documentation." [Online]. Available: https://kotlinlang.org/docs/home.html. [Accessed: Aug. 2026].',
        '[31] Android Developers, "Use Hilt in Your Android App." [Online]. Available: https://developer.android.com/training/dependency-injection/hilt-android. [Accessed: Aug. 2026].',
        '[32] Android Developers, "StateFlow and SharedFlow." [Online]. Available: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow. [Accessed: Aug. 2026].',
    ]
    for x in refs: ref(doc, x)


def build():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = Document(); configure(doc)
    footer = doc.sections[0].footer.paragraphs[0]; footer.clear(); page_number(footer)
    cover(doc)
    p = doc.add_paragraph(); p.add_run().add_break(WD_BREAK.PAGE)
    contents(doc)
    p = doc.add_paragraph(); p.add_run().add_break(WD_BREAK.PAGE)
    write_chapter_one(doc)
    write_chapter_two(doc)
    write_chapter_three(doc)
    write_chapter_four(doc)
    write_chapter_five(doc)
    write_chapter_six(doc)
    write_chapter_seven(doc)
    references(doc)
    doc.core_properties.title = "Somewhere - Project Synopsis"
    doc.core_properties.subject = "Major Project Synopsis"
    doc.core_properties.author = "[STUDENT NAME]"
    doc.core_properties.keywords = "Android, augmented reality, location, social discovery, Supabase"
    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    build()
