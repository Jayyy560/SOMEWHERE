from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt


ROOT = Path(__file__).resolve().parents[1]
DOCX_OUT = ROOT / "output" / "docx" / "Somewhere_Project_Synopsis.docx"

FONT = "Times New Roman"
BODY_SIZE = 12


def set_run_font(run, size=BODY_SIZE, bold=False, italic=False):
    run.font.name = FONT
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), FONT)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), FONT)
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), FONT)
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic


def set_a4_layout(section):
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.left_margin = Cm(3.5)
    section.top_margin = Cm(2.5)
    section.right_margin = Cm(1.25)
    section.bottom_margin = Cm(1.25)
    section.header_distance = Cm(0.8)
    section.footer_distance = Cm(0.7)


def set_double_spacing(paragraph, before=0, after=0, first_indent=False):
    fmt = paragraph.paragraph_format
    fmt.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    fmt.space_before = Pt(before)
    fmt.space_after = Pt(after)
    if first_indent:
        fmt.first_line_indent = Cm(0.75)


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run()
    set_run_font(run, size=10)
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instruction = OxmlElement("w:instrText")
    instruction.set(qn("xml:space"), "preserve")
    instruction.text = " PAGE "
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instruction, end])


def restart_page_numbering(section, start=1):
    sect_pr = section._sectPr
    pg_num = sect_pr.find(qn("w:pgNumType"))
    if pg_num is None:
        pg_num = OxmlElement("w:pgNumType")
        sect_pr.append(pg_num)
    pg_num.set(qn("w:start"), str(start))


def add_centered(doc, text, size=BODY_SIZE, bold=False, italic=False, after=0):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(after)
    r = p.add_run(text)
    set_run_font(r, size=size, bold=bold, italic=italic)
    return p


def add_spacer(doc, points):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(points)
    return p


def add_heading(doc, text, page_break_before=False):
    p = doc.add_paragraph(style="Heading 1")
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.keep_with_next = True
    p.paragraph_format.page_break_before = page_break_before
    p.add_run(text)
    return p


def add_subheading(doc, text):
    p = doc.add_paragraph(style="Heading 2")
    p.paragraph_format.keep_with_next = True
    p.add_run(text)
    return p


def add_body(doc, text, first_indent=True):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    set_double_spacing(p, first_indent=first_indent)
    p.add_run(text)
    return p


def add_numbered(doc, text):
    p = doc.add_paragraph(style="List Number")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    set_double_spacing(p)
    p.paragraph_format.left_indent = Cm(0.75)
    p.paragraph_format.first_line_indent = Cm(-0.5)
    p.add_run(text)
    return p


def add_bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    set_double_spacing(p)
    p.paragraph_format.left_indent = Cm(0.75)
    p.paragraph_format.first_line_indent = Cm(-0.5)
    p.add_run(text)
    return p


def add_reference(doc, text):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    set_double_spacing(p, after=6)
    p.paragraph_format.left_indent = Cm(0.75)
    p.paragraph_format.first_line_indent = Cm(-0.75)
    p.add_run(text)
    return p


def add_toc_entry(doc, label, page):
    p = doc.add_paragraph(style="Normal")
    set_double_spacing(p)
    p.paragraph_format.tab_stops.add_tab_stop(
        Cm(15.7), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS
    )
    p.add_run(label)
    p.add_run(f"\t{page}")
    return p


def add_page_break(doc):
    p = doc.add_paragraph()
    p.add_run().add_break(WD_BREAK.PAGE)


def configure_styles(doc):
    normal = doc.styles["Normal"]
    normal.font.name = FONT
    normal._element.rPr.rFonts.set(qn("w:ascii"), FONT)
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
    normal.font.size = Pt(BODY_SIZE)
    normal.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(0)

    h1 = doc.styles["Heading 1"]
    h1.font.name = FONT
    h1._element.rPr.rFonts.set(qn("w:ascii"), FONT)
    h1._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
    h1.font.size = Pt(14)
    h1.font.bold = True
    h1.font.color.rgb = None
    h1.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    h1.paragraph_format.space_before = Pt(0)
    h1.paragraph_format.space_after = Pt(6)
    h1.paragraph_format.keep_with_next = True

    h2 = doc.styles["Heading 2"]
    h2.font.name = FONT
    h2._element.rPr.rFonts.set(qn("w:ascii"), FONT)
    h2._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
    h2.font.size = Pt(12)
    h2.font.bold = False
    h2.font.italic = False
    h2.font.color.rgb = None
    h2.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
    h2.paragraph_format.space_before = Pt(4)
    h2.paragraph_format.space_after = Pt(0)
    h2.paragraph_format.keep_with_next = True

    for name in ("List Number", "List Bullet"):
        style = doc.styles[name]
        style.font.name = FONT
        style._element.rPr.rFonts.set(qn("w:ascii"), FONT)
        style._element.rPr.rFonts.set(qn("w:hAnsi"), FONT)
        style.font.size = Pt(BODY_SIZE)
        style.paragraph_format.line_spacing_rule = WD_LINE_SPACING.DOUBLE
        style.paragraph_format.space_before = Pt(0)
        style.paragraph_format.space_after = Pt(0)


def build():
    DOCX_OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = Document()
    configure_styles(doc)
    set_a4_layout(doc.sections[0])

    # Title page - follows the supplied sample while retaining all requested particulars.
    add_spacer(doc, 8)
    add_centered(doc, "SOMEWHERE", size=24, bold=True, after=4)
    add_centered(
        doc,
        "A LOCATION-BASED AUGMENTED REALITY SOCIAL DISCOVERY APPLICATION",
        size=16,
        bold=True,
        after=18,
    )
    add_centered(doc, "PROJECT SYNOPSIS", size=14, bold=True, after=2)
    add_centered(doc, "OF MAJOR PROJECT", size=12, after=12)
    add_centered(doc, "B.TECH. (COMPUTER SCIENCE AND ENGINEERING)", size=14, bold=True, after=4)
    add_centered(doc, "DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING", size=16, after=18)

    add_centered(doc, "SUBMITTED BY", size=12, bold=True, after=2)
    add_centered(doc, "[STUDENT NAME]", size=14, after=0)
    add_centered(doc, "University Roll No.: [UNIVERSITY ROLL NO.]", size=12)
    add_centered(doc, "Class Roll No.: [CLASS ROLL NO.]", size=12)
    add_centered(doc, "Branch: Computer Science and Engineering | Batch: [BATCH]", size=12, after=10)

    add_centered(doc, "SUBMITTED TO", size=12, bold=True, after=2)
    add_centered(doc, "[NAME OF PROJECT SUPERVISOR / GUIDE]", size=14, after=14)
    add_centered(doc, "Proposed Topic: Location-Based Augmented Reality and Social Computing", size=12, after=14)
    add_centered(doc, "AUGUST 2026", size=12, after=8)
    add_centered(doc, "[INSERT UNIVERSITY LOGO]", size=12, italic=True, after=4)
    add_centered(doc, "[UNIVERSITY NAME]", size=16, bold=True)

    # Contents page.
    add_heading(doc, "TABLE OF CONTENTS", page_break_before=True)
    add_toc_entry(doc, "1. Introduction", "1")
    add_toc_entry(doc, "   1.1 Rationale", "1")
    add_toc_entry(doc, "   1.2 Objectives", "1")
    add_toc_entry(doc, "2. Literature Review", "2")
    add_toc_entry(doc, "3. Feasibility Study", "3")
    add_toc_entry(doc, "4. Methodology / Planning of Work", "4")
    add_toc_entry(doc, "5. Facilities Required for Proposed Work", "5")
    add_toc_entry(doc, "6. Expected Outcomes", "5")
    add_toc_entry(doc, "References", "6")

    # Main matter uses a new section and restarts numbering at page 1.
    main = doc.add_section(WD_SECTION.NEW_PAGE)
    set_a4_layout(main)
    main.header.is_linked_to_previous = False
    main.footer.is_linked_to_previous = False
    restart_page_numbering(main, 1)
    main.footer.paragraphs[0].clear()
    add_page_number(main.footer.paragraphs[0])

    # 1. Introduction - one page.
    add_heading(doc, "1. INTRODUCTION")
    add_body(
        doc,
        "Somewhere is a native Android application that turns physical locations into a shared digital layer. Users create multimedia messages called Drops by capturing a photograph, adding a short message and attaching the content to a measured geographical coordinate. Other users discover nearby Drops by moving through the real world and viewing them through an augmented-reality camera, compass-based overlay or accessible list. The project therefore combines place-based storytelling, social interaction and outdoor exploration.",
    )
    add_body(
        doc,
        "The implementation uses Kotlin, Jetpack Compose, MVVM, StateFlow and Hilt. CameraX captures images; fused location and sensors supply position and heading; and ARCore anchors content when supported. Room provides local caching, while Supabase supplies authentication, PostgreSQL records, media storage and realtime events. Google Maps supports Trip Mode and OpenRouter provides optional summaries. Key terms are geofencing, spatial anchoring and location uncertainty, meaning the reported error radius of a GPS fix.",
    )
    add_subheading(doc, "1.1 Rationale")
    add_body(
        doc,
        "Conventional social media often removes content from the place that gave it meaning. Somewhere provides a location-first alternative in which a story, memory or digital file is encountered in context. Accuracy-aware proximity logic reduces false locking from GPS error, while moderation, blocking and permission controls address practical deployment concerns.",
    )
    add_subheading(doc, "1.2 Objectives")
    add_bullet(doc, "To create persistent or time-limited multimedia Drops at measured coordinates.")
    add_bullet(doc, "To discover Drops through distance, bearing, GPS accuracy and AR/compass views.")
    add_bullet(doc, "To provide local/cloud persistence, realtime interaction and route discovery.")
    add_bullet(doc, "To evaluate feasibility, usability and limitations on consumer Android devices.")

    # 2. Literature review - one page.
    add_heading(doc, "2. LITERATURE REVIEW", page_break_before=True)
    add_body(
        doc,
        "Azuma [1] defines augmented reality through the real-time combination of real and virtual content with three-dimensional registration. The survey identifies tracking and registration error as central challenges. Somewhere responds by treating AR as an enhancement rather than a single point of failure and retaining a sensor-based fallback when stable tracking is unavailable.",
    )
    add_body(
        doc,
        "Feiner et al. [2] demonstrated an early mobile AR touring system that combined outdoor position and orientation sensing with contextual campus information. Their work establishes the value of situated information but also reveals the hardware and calibration burden of outdoor AR. Somewhere implements the same broad idea on a smartphone using fused location, rotation sensing and ARCore.",
    )
    add_body(
        doc,
        "Tursi, Deplano and Ruffo [3] presented AiRCacher, an AR-enhanced virtual geocaching system designed to motivate outdoor movement and generate geolocated social data. It validates the hide-and-seek interaction model. Somewhere extends this concept from caches to photographs, text, audio, attachments, reactions, comments and moving Hitchhiker Drops.",
    )
    add_body(
        doc,
        "Liu and Fuhrmann [4] examined the convergence of augmented reality and location-based social networks, highlighting multimedia storytelling, visualisation, functionality and ethics. Their analysis directly supports Somewhere's location-tagged user-generated content, while also motivating privacy, moderation, accuracy disclosure and careful public deployment.",
    )
    add_body(
        doc,
        "Althoff, White and Horvitz [5] reported that sustained engagement with a geocentric mobile game was associated with increased physical activity over the observed period. The result suggests that location-based interaction can encourage movement, although Somewhere does not claim a health effect without a dedicated user study. Collectively, the literature supports a mobile, movement-oriented and socially authored AR system while identifying tracking quality, safety, privacy and long-term engagement as evaluation priorities.",
    )

    # 3. Feasibility - one page.
    add_heading(doc, "3. FEASIBILITY STUDY", page_break_before=True)
    add_subheading(doc, "3.1 Technical Feasibility")
    add_body(
        doc,
        "The repository builds with Java 17 for Android API 26 or above. CameraX, fused location, sensors, optional ARCore, Room, Hilt, Supabase and Google Maps are integrated. Unsupported devices retain compass/list discovery, and the current location, trip-parser and theme unit tests pass.",
    )
    add_subheading(doc, "3.2 Operational Feasibility")
    add_body(
        doc,
        "The workflow uses familiar actions: sign in, capture, describe, confirm location and publish. SavedStateHandle retains the creation draft, and explicit loading, retry, permission and offline states support recovery. Users can report content, block authors and delete their own Drops.",
    )
    add_subheading(doc, "3.3 Economic Feasibility")
    add_body(
        doc,
        "Open-source Android libraries and service free tiers make a student prototype economical. A development laptop and Android test phone are sufficient. Production costs for storage, realtime traffic, Maps, AI, monitoring and moderation can be constrained through compression, caching, query limits and quotas.",
    )
    add_subheading(doc, "3.4 Schedule Feasibility")
    add_body(
        doc,
        "The work is iterative: requirements and data modelling; authentication and creation; spatial discovery; social and trip features; testing; and documentation. The main functions are implemented, leaving field validation, security hardening, usability evaluation and deployment preparation.",
    )
    add_subheading(doc, "3.5 Legal, Privacy and Safety Feasibility")
    add_body(
        doc,
        "Camera, location, audio and notifications require informed consent. Production must enforce row-level security, protected storage, data minimisation, moderation/retention rules, safe placement and removal of verbose network logging.",
    )

    # 4. Methodology - one page.
    add_heading(doc, "4. METHODOLOGY / PLANNING OF WORK", page_break_before=True)
    add_body(
        doc,
        "The project follows an applied design-and-development methodology. The unit of analysis is a Drop across creation, placement, retrieval, unlock, interaction and moderation. Repository inspection, unit tests and controlled device/field trials provide validation data. Work is organised as follows:",
    )
    add_numbered(doc, "Requirements and modelling: define users, permissions, Drop attributes, privacy risks, functional states and acceptance conditions; map structured data to Room and Supabase while storing media separately.")
    add_numbered(doc, "Interface and architecture: build a single-activity Compose app with navigation, ViewModels, StateFlow, repositories and Hilt; expose lifecycle-aware permission, network, camera and location states.")
    add_numbered(doc, "Creation and placement: capture with CameraX; collect up to twelve fresh samples for ten seconds; reject stale/poor readings; calculate an inverse-accuracy-squared centre; and store accuracy, time and provider.")
    add_numbered(doc, "Persistence and synchronisation: compress and upload media, insert the Supabase record, cache relevant Drops in Room, and reconcile comments, likes, notifications and nearby changes through realtime subscriptions.")
    add_numbered(doc, "Discovery and interaction: query within 500 m, calculate distance/bearing, show up to fifteen overlays, and unlock at 30 m plus a combined accuracy allowance capped at 25 m. Use ARCore anchors or the compass/list fallback.")
    add_numbered(doc, "Evaluation: test Trip Mode, geofences, AI summaries, exact-spot matching, expiry, blocking and Hitchhiker transfer. Compare expected and observed accuracy, latency, compatibility and recovery behaviour, then refine the design.")

    # 5 and 6 share one page, as requested by the source format.
    add_heading(doc, "5. FACILITIES REQUIRED FOR PROPOSED WORK", page_break_before=True)
    add_body(
        doc,
        "The software facilities are Android Studio, the Android SDK, Kotlin, Gradle, JDK 17, Jetpack Compose, CameraX, ARCore/SceneView, Room, Hilt, Supabase client libraries, Google Maps services, OpenRouter access, Git and JUnit. Supabase provides authentication, PostgreSQL, object storage and realtime channels; an optional Flask/Ollama service supports local AI experiments. Hardware consists of a development computer, internet access and at least one Android 8.0+ smartphone with camera, GPS, accelerometer and compass. An ARCore-supported phone and multiple devices/accounts are desirable for anchor stability, realtime and compatibility tests. Outdoor and indoor test locations are required to compare GPS conditions, while configured API credentials and a protected test backend are needed for end-to-end trials.",
    )
    add_heading(doc, "6. EXPECTED OUTCOMES")
    add_body(
        doc,
        "The expected outcome is a working Android prototype in which authenticated users can create photograph-, text-, audio- and file-based Drops, choose permanence or expiry, confirm an accuracy-aware location and publish to a shared backend. Nearby users should receive a distance- and direction-aware view, unlock content within a bounded physical radius, inspect the original viewpoint, react and comment in realtime, receive notifications, and use route-based discovery. The system should remain usable when ARCore, the camera, the network or a high-quality location fix is unavailable by presenting explicit fallback and retry states. Technical outputs will include the application source, Room schema migrations, Supabase integration, unit tests and documentation of algorithms and limitations. Evaluation is expected to show the practical viability of location-based storytelling while identifying measurable constraints involving GPS uncertainty, AR registration, energy use, privacy, moderation, service cost and device compatibility.",
    )

    # References - IEEE style.
    add_heading(doc, "REFERENCES", page_break_before=True)
    add_reference(doc, '[1] R. T. Azuma, "A survey of augmented reality," Presence: Teleoperators and Virtual Environments, vol. 6, no. 4, pp. 355-385, Aug. 1997, doi: 10.1162/pres.1997.6.4.355.')
    add_reference(doc, '[2] S. Feiner, B. MacIntyre, T. H\u00f6llerer and A. Webster, "A touring machine: Prototyping 3D mobile augmented reality systems for exploring the urban environment," in Proc. 1st Int. Symp. Wearable Computers, Cambridge, MA, USA, 1997, pp. 74-81.')
    add_reference(doc, '[3] G. Tursi, M. Deplano and G. Ruffo, "AiRCacher: Virtual geocaching powered with augmented reality," in Proc. 25th ACM Conf. Hypertext and Social Media, Santiago, Chile, 2014, pp. 330-332, doi: 10.1145/2631775.2631778.')
    add_reference(doc, '[4] C. Liu and S. Fuhrmann, "Enriching the GIScience research agenda: Fusing augmented reality and location-based social networks," Transactions in GIS, vol. 22, no. 3, pp. 775-788, Jun. 2018, doi: 10.1111/tgis.12345.')
    add_reference(doc, '[5] T. Althoff, R. W. White and E. Horvitz, "Influence of Pok\u00e9mon Go on physical activity: Study and implications," Journal of Medical Internet Research, vol. 18, no. 12, Art. no. e315, Dec. 2016, doi: 10.2196/jmir.6759.')
    add_reference(doc, '[6] Google, "Guide to app architecture," Android Developers. [Online]. Available: https://developer.android.com/topic/architecture. [Accessed: Aug. 27, 2026].')

    doc.core_properties.title = "Somewhere - Project Synopsis"
    doc.core_properties.subject = "Major Project Synopsis"
    doc.core_properties.author = "[Student Name]"
    doc.core_properties.keywords = "Android, augmented reality, location-based social network, Supabase, Somewhere"
    doc.save(DOCX_OUT)
    print(DOCX_OUT)


if __name__ == "__main__":
    build()
