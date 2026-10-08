#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script generating formal University of Da Lat Project Report (.docx)
following exact guidelines:
- Paper: A4 (Top: 2.0cm, Bottom: 2.0cm, Left: 3.0cm, Right: 2.0cm)
- Font: Times New Roman
- Body: 13pt, 1.3 lines spacing, Justified, 6pt after
- Headings: 16pt / 14pt / 13pt bold
- Tables: Header styled, centered, padded
"""

import os
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def set_table_borders(table, color="D1D5DB", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(f'''
        <w:tblBorders {nsdecls("w")}>
            <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:left w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:right w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
            <w:insideV w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>
        </w:tblBorders>
    ''')
    tblPr.append(borders)

def build_report():
    doc = Document()

    # Page Margins (Chuẩn ĐH Đà Lạt: Top 2cm, Bottom 2cm, Left 3cm, Right 2cm)
    for section in doc.sections:
        section.top_margin = Cm(2.0)
        section.bottom_margin = Cm(2.0)
        section.left_margin = Cm(3.0)
        section.right_margin = Cm(2.0)
        section.page_width = Cm(21.0)
        section.page_height = Cm(29.7)

    # Styles
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Times New Roman'
    style_normal.font.size = Pt(13)
    style_normal.font.color.rgb = RGBColor(30, 41, 59)
    style_normal.paragraph_format.line_spacing = 1.3
    style_normal.paragraph_format.space_after = Pt(6)
    style_normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    def add_p(text, bold=False, italic=False, size=13, align=WD_ALIGN_PARAGRAPH.JUSTIFY, space_before=0, space_after=6, color=RGBColor(30, 41, 59)):
        p = doc.add_paragraph()
        p.alignment = align
        p.paragraph_format.space_before = Pt(space_before)
        p.paragraph_format.space_after = Pt(space_after)
        p.paragraph_format.line_spacing = 1.3
        run = p.add_run(text)
        run.bold = bold
        run.italic = italic
        run.font.name = 'Times New Roman'
        run.font.size = Pt(size)
        run.font.color.rgb = color
        return p

    def add_h1(title):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(18)
        p.paragraph_format.space_after = Pt(8)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(title.upper())
        run.bold = True
        run.font.name = 'Times New Roman'
        run.font.size = Pt(15)
        run.font.color.rgb = RGBColor(15, 23, 42)
        return p

    def add_h2(title):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(title)
        run.bold = True
        run.font.name = 'Times New Roman'
        run.font.size = Pt(13.5)
        run.font.color.rgb = RGBColor(30, 58, 138)
        return p

    def add_h3(title):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(title)
        run.bold = True
        run.italic = True
        run.font.name = 'Times New Roman'
        run.font.size = Pt(13)
        run.font.color.rgb = RGBColor(51, 65, 85)
        return p

    def create_table(headers, rows_data, col_widths=None):
        table = doc.add_table(rows=len(rows_data) + 1, cols=len(headers))
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        set_table_borders(table)

        # Header Row
        hdr_cells = table.rows[0].cells
        for idx, header_text in enumerate(headers):
            cell = hdr_cells[idx]
            cell.text = header_text
            set_cell_background(cell, "F1F5F9")
            set_cell_margins(cell, top=120, bottom=120, left=150, right=150)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.space_before = Pt(0)
            for r in p.runs:
                r.bold = True
                r.font.name = 'Times New Roman'
                r.font.size = Pt(12)
                r.font.color.rgb = RGBColor(15, 23, 42)

        # Data Rows
        for r_idx, row in enumerate(rows_data):
            row_cells = table.rows[r_idx + 1].cells
            bg_color = "FFFFFF" if r_idx % 2 == 0 else "F8FAFC"
            for c_idx, val in enumerate(row):
                cell = row_cells[c_idx]
                cell.text = str(val)
                set_cell_background(cell, bg_color)
                set_cell_margins(cell, top=100, bottom=100, left=140, right=140)
                p = cell.paragraphs[0]
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT if c_idx > 0 and len(str(val)) > 15 else WD_ALIGN_PARAGRAPH.CENTER
                p.paragraph_format.space_after = Pt(0)
                p.paragraph_format.space_before = Pt(0)
                for r in p.runs:
                    r.font.name = 'Times New Roman'
                    r.font.size = Pt(12)
                    r.font.color.rgb = RGBColor(30, 41, 59)

        if col_widths:
            for row in table.rows:
                for c_idx, w in enumerate(col_widths):
                    row.cells[c_idx].width = Cm(w)

        doc.add_paragraph().paragraph_format.space_after = Pt(6)
        return table

    # =========================================================================
    # 1. TRANG BÌA (Cover Page)
    # =========================================================================
    add_p("TRƯỜNG ĐẠI HỌC ĐÀ LẠT", bold=True, size=15, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=10, space_after=2)
    add_p("KHOA CÔNG NGHỆ THÔNG TIN", bold=True, size=15, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=0, space_after=30)

    add_p("— ❖ —", size=14, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=40)

    add_p("BÁO CÁO ĐỒ ÁN MÔN HỌC", bold=True, size=16, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=6, color=RGBColor(15, 23, 42))
    add_p("HỌC PHẦN: LẬP TRÌNH JAVA NÂNG CAO", bold=True, size=14, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=25, color=RGBColor(30, 58, 138))

    add_p("ĐỀ TÀI:", bold=True, size=14, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=8)
    add_p("NGHIÊN CỨU THUẬT TOÁN DHOPM VÀ XÂY DỰNG ỨNG DỤNG\nTRỰC QUAN HÓA KHAI PHÁ MẪU ĐỘ CHIẾM DỤNG CAO\nTRÊN LUỒNG DỮ LIỆU",
          bold=True, size=16, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=8, color=RGBColor(15, 23, 42))
    add_p("(Damped Window Based High Occupancy Pattern Mining with One Scanning of Data Streams)",
          italic=True, size=13, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=80, color=RGBColor(71, 85, 105))

    add_p("Giảng viên hướng dẫn:  ThS. Đoàn Minh Khuê", bold=True, size=13, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=6)
    add_p("Sinh viên thực hiện:      2312741 – Nguyễn Thanh Tâm (CTK47-PM / CTK45)", size=13, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=4)
    add_p("Thành viên phối hợp:    Nguyễn Hữu Trung Sơn (Phụ trách Core Engine)", size=13, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=60)

    add_p("Đà Lạt, tháng 10 năm 2026", bold=True, size=13, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=0)

    doc.add_page_break()

    # =========================================================================
    # 2. NHẬN XÉT CỦA GIÁO VIÊN HƯỚNG DẪN
    # =========================================================================
    add_h1("NHẬN XÉT CỦA GIÁO VIÊN HƯỚNG DẪN")
    add_p("...................................................................................................................................................................", space_before=15, space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)
    add_p("...................................................................................................................................................................", space_after=12)

    add_p("Đà Lạt, ngày … tháng … năm 2026", italic=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_before=30, space_after=6)
    add_p("Giáo viên hướng dẫn", bold=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=4)
    add_p("(Ký tên và ghi rõ họ tên)", italic=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=50)
    add_p("ThS. Đoàn Minh Khuê", bold=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=0)

    doc.add_page_break()

    # =========================================================================
    # 3. ĐỀ CƯƠNG THỰC HIỆN ĐỒ ÁN
    # =========================================================================
    add_p("Trường Đại Học Đà Lạt\nKhoa Công Nghệ Thông Tin", bold=True, size=13, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=8)
    add_p("---- ❖ ----", size=13, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=14)
    add_h1("ĐỀ CƯƠNG THỰC HIỆN ĐỒ ÁN")
    add_p("Tên đề tài: Nghiên cứu thuật toán DHOPM và xây dựng ứng dụng trực quan hóa khai phá mẫu độ chiếm dụng cao trên luồng dữ liệu (DHOPM Stream Visualizer)", bold=True, space_after=12)

    add_p("Sinh viên thực hiện:", bold=True, space_after=6)
    create_table(
        ["STT", "Họ và Tên", "MSSV", "Lớp", "Email Liên hệ"],
        [
            ["1", "Nguyễn Thanh Tâm", "2312741", "CTK47-PM", "2312741@dlu.edu.vn"],
            ["2", "Nguyễn Hữu Trung Sơn", "-", "CTK45-PM", "trungson@dlu.edu.vn"]
        ],
        [1.2, 5.0, 2.5, 2.8, 4.5]
    )

    add_p("Giảng viên hướng dẫn: ThS. Đoàn Minh Khuê", bold=True, space_before=10, space_after=10)

    add_h2("1. Mục tiêu đề tài")
    add_p("- Nghiên cứu lý thuyết học thuật: Làm chủ bài toán Khai phá mẫu độ chiếm dụng cao (HOPM) trên luồng dữ liệu thời gian thực theo bài báo khoa học quốc tế EAAI 2026. Nắm vững mô hình suy giảm Damped Window (0 < f <= 1), cận trên DUBO cắt tỉa an toàn và cấu trúc nén Global DHO-List một lần quét.")
    add_p("- Xây dựng ứng dụng phần mềm trực quan hóa: Phát triển hoàn chỉnh phần mềm DHOPM Stream Visualizer bằng Java 21 và JavaFX, gồm 6 Tab chức năng trực quan hóa cấu trúc cây DHO-Tree, bảng kết quả với Support (tx / %), bộ khảo sát đa ngưỡng Benchmark Sweep dựng biểu đồ bài báo (Fig 6, 11, 13), tích hợp Tson/SPMF Engine với tiến trình ngầm, nút dừng an toàn và ước tính ETA, quản lý lịch sử khai phá (Memento) và nhật ký tính toán chi tiết (Calculation Logger).")
    add_p("- Áp dụng kiến trúc chuẩn công nghiệp: Triển khai thành công 9 Mẫu thiết kế phần mềm GoF, phân tầng MVC sạch, đảm bảo 100% ca kiểm thử tự động (Unit Test) vượt qua kiểm thử.")

    add_h2("2. Nội dung đề tài")
    add_p("Chương 1: Tổng quan về đề tài")
    add_p("Chương 2: Cơ sở lý thuyết và công nghệ")
    add_p("Chương 3: Phân tích và thiết kế hệ thống")
    add_p("Chương 4: Xây dựng hệ thống và hiện thực hóa ứng dụng")
    add_p("Chương 5: Kết quả thực nghiệm và đối soát bài báo gốc")
    add_p("Kết luận và hướng phát triển")

    add_h2("3. Phần mềm và công cụ sử dụng")
    add_p("- Công nghệ sử dụng: Java 21 LTS (OpenJDK), JavaFX 21, Apache Maven 3.9, Thư viện SPMF Data Mining, JUnit 5.")
    add_p("- Công cụ phát triển: IntelliJ IDEA, Git, GitHub (kho mã nguồn https://github.com/2312741-sudo/javanc.git), macOS / Linux.")

    add_h2("4. Dự kiến kết quả đạt được")
    add_p("- Hoàn thành ứng dụng phần mềm JavaFX trực quan, thẩm mỹ, mượt mà và đầy đủ tính năng.")
    add_p("- Đạt 100% ca kiểm thử tự động (48/48 test cases PASS), khớp tuyệt đối với số liệu công trình khoa học gốc EAAI 2026.")
    add_p("- Nâng cao kỹ năng phân tích thiết kế, làm việc nhóm, áp dụng mẫu thiết kế hướng đối tượng và viết tài liệu học thuật.")

    add_h2("5. Tài liệu tham khảo chính")
    add_p("[1] M. Cho, H. Kim, P. Fournier-Viger, and U. Yun, 'Damped window based high occupancy pattern mining with one scanning of data streams', Engineering Applications of Artificial Intelligence (EAAI), vol. 174, p. 114511, 2026.")
    add_p("[2] E. Gamma, R. Helm, R. Johnson, and J. Vlissides, 'Design Patterns: Elements of Reusable Object-Oriented Software', Addison-Wesley, 1994.")

    # Bảng chữ ký
    add_p("\n", space_after=10)
    sig_table = doc.add_table(rows=2, cols=2)
    sig_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for r in sig_table.rows:
        for c in r.cells:
            set_cell_background(c, "FFFFFF")
            set_cell_margins(c, top=80, bottom=80, left=100, right=100)
    
    # Cell 0,0: GVHD
    p = sig_table.rows[0].cells[0].paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Giáo viên hướng dẫn\n\n\n\n\nThS. Đoàn Minh Khuê")
    r.bold = True
    r.font.name = 'Times New Roman'
    r.font.size = Pt(12)

    # Cell 0,1: SV
    p = sig_table.rows[0].cells[1].paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Đà Lạt, ngày 08 tháng 10 năm 2026\nSinh viên thực hiện\n\n\n\nNguyễn Thanh Tâm\nNguyễn Hữu Trung Sơn")
    r.bold = True
    r.font.name = 'Times New Roman'
    r.font.size = Pt(12)

    # Cell 1,0: BCN Khoa
    p = sig_table.rows[1].cells[0].paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("\nBCN Khoa\n(Ký tên)")
    r.bold = True
    r.font.name = 'Times New Roman'
    r.font.size = Pt(12)

    # Cell 1,1: Bộ môn
    p = sig_table.rows[1].cells[1].paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("\nTổ trưởng Bộ môn\n(Ký tên)")
    r.bold = True
    r.font.name = 'Times New Roman'
    r.font.size = Pt(12)

    doc.add_page_break()

    # =========================================================================
    # 4. LỜI CẢM ƠN
    # =========================================================================
    add_h1("LỜI CẢM ƠN")
    add_p("Trước hết, nhóm chúng em xin bày tỏ lòng biết ơn sâu sắc và chân thành nhất đến quý Thầy Cô trong Khoa Công nghệ Thông tin – Trường Đại học Đà Lạt, những người đã nhiệt tình giảng dạy, trang bị cho chúng em những nền tảng tri thức chuyên sâu và phương pháp tư duy khoa học vững chắc trong suốt những năm học tập tại trường.", space_before=10)
    add_p("Đặc biệt, nhóm chúng em xin trân trọng gửi lời cảm ơn sâu sắc nhất đến Thầy ThS. Đoàn Minh Khuê, giảng viên phụ trách hướng dẫn đồ án. Sự định hướng tận tâm, phương pháp sư phạm gợi mở và những lời khuyên chuyên môn sâu sắc của Thầy đã giúp nhóm từng bước tháo gỡ những trở ngại toán học phức tạp, hoàn thiện kiến trúc ứng dụng từ một bài báo khoa học quốc tế đến một sản phẩm phần mềm trực quan hóa hoàn chỉnh.")
    add_p("Mặc dù nhóm đã nỗ lực hết mình để triển khai đồ án một cách chỉn chu, bài bản cả về mặt lý thuyết giải thuật lẫn công nghệ hiện thực, song do kiến thức và kinh nghiệm còn nhiều hạn chế, đề tài chắc chắn khó tránh khỏi những thiếu sót nhất định. Nhóm chúng em rất mong nhận được những ý kiến đóng góp quý báu từ quý Thầy Cô trong Hội đồng chấm đồ án và các bạn sinh viên để sản phẩm ngày càng được hoàn thiện hơn nữa.")
    add_p("Chúng em xin kính chúc quý Thầy Cô luôn dồi dào sức khỏe, hạnh phúc và gặt hái thêm nhiều thành tựu rực rỡ trong sự nghiệp nghiên cứu khoa học và giảng dạy cao quý!")

    add_p("Đà Lạt, tháng 10 năm 2026", italic=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_before=20, space_after=4)
    add_p("Nhóm sinh viên thực hiện", bold=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=4)
    add_p("Nguyễn Thanh Tâm – Nguyễn Hữu Trung Sơn", italic=True, align=WD_ALIGN_PARAGRAPH.RIGHT, space_after=0)

    doc.add_page_break()

    # =========================================================================
    # 5. MỤC LỤC
    # =========================================================================
    add_h1("MỤC LỤC")
    toc_data = [
        ("NHẬN XÉT CỦA GIÁO VIÊN HƯỚNG DẪN", "2"),
        ("ĐỀ CƯƠNG THỰC HIỆN ĐỒ ÁN", "3"),
        ("LỜI CẢM ƠN", "6"),
        ("DANH MỤC HÌNH ẢNH", "8"),
        ("DANH MỤC BẢNG BIỂU", "10"),
        ("MỞ ĐẦU", "13"),
        ("CHƯƠNG 1. TỔNG QUAN VỀ ĐỀ TÀI", "14"),
        ("    1.1. Giới thiệu đề tài", "14"),
        ("    1.2. Lý do chọn đề tài", "14"),
        ("    1.3. Mục tiêu đề tài", "15"),
        ("    1.4. Phạm vi nghiên cứu", "16"),
        ("CHƯƠNG 2. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ", "17"),
        ("    2.1. Bài toán khai phá mẫu độ chiếm dụng cao (HOPM)", "17"),
        ("    2.2. Khai phá trên luồng dữ liệu và mô hình Damped Window", "18"),
        ("    2.3. Cơ sở toán học thuật toán DHOPM", "19"),
        ("        2.3.1. Độ đo Damped Occupancy (DO)", "19"),
        ("        2.3.2. Cận trên DUBO và tính chất cắt tỉa an toàn", "20"),
        ("        2.3.3. Cấu trúc danh sách Global DHO-List một lần quét", "21"),
        ("    2.4. Công nghệ sử dụng", "22"),
        ("        2.4.1. Ngôn ngữ lập trình Java 21 / 25", "22"),
        ("        2.4.2. Framework JavaFX 21", "23"),
        ("        2.4.3. Công cụ quản lý dự án Apache Maven", "24"),
        ("        2.4.4. Thư viện khai phá dữ liệu SPMF và Core Engine", "24"),
        ("        2.4.5. Git và GitHub", "25"),
        ("CHƯƠNG 3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG", "26"),
        ("    3.1. Phân tích yêu cầu", "26"),
        ("        3.1.1. Yêu cầu chức năng", "26"),
        ("        3.1.2. Yêu cầu phi chức năng", "27"),
        ("    3.2. Thiết kế hệ thống", "28"),
        ("        3.2.1. Kiến trúc phân tầng MVC và 9 Mẫu thiết kế GoF", "28"),
        ("        3.2.2. Sơ đồ Use Case của hệ thống", "32"),
        ("        3.2.3. Các bảng đặc tả Use Case chi tiết", "34"),
        ("        3.2.4. Thiết kế cấu trúc dữ liệu Model", "48"),
        ("CHƯƠNG 4. XÂY DỰNG HỆ THỐNG VÀ HIỆN THỰC HÓA", "52"),
        ("    4.1. Cấu trúc thư mục và tổ chức mã nguồn", "52"),
        ("    4.2. Hiện thực hóa các phân hệ lõi nghiệp vụ", "54"),
        ("    4.3. Xây dựng giao diện trực quan hóa JavaFX", "57"),
        ("        4.3.1. Tab 1 & Tab 2: Khai phá Stream & Cây DHO-Tree", "57"),
        ("        4.3.2. Tab 3: Khảo sát Benchmark Sweep đa ngưỡng minSup", "59"),
        ("        4.3.3. Tab 4: Phân hệ khai phá Big Data Tson / SPMF", "61"),
        ("        4.3.4. Tab 5: Lịch sử khai phá và đa biểu đồ xu hướng", "63"),
        ("        4.3.5. Tab 6: Nhật ký tính toán chi tiết từng bước", "65"),
        ("    4.4. Xử lý đa luồng bất đồng bộ và kiểm soát an toàn bộ nhớ", "67"),
        ("CHƯƠNG 5. KẾT QUẢ THỰC NGHIỆM VÀ ĐỐI SOÁT BÀI BÁO GỐC", "69"),
        ("    5.1. Bộ dữ liệu thực nghiệm", "69"),
        ("    5.2. Kết quả kiểm thử tự động toàn diện (48 Test Cases)", "70"),
        ("    5.3. Đối soát tính đúng đắn với công trình gốc (Golden Tests)", "71"),
        ("    5.4. Đánh giá hiệu năng và hiệu quả cắt tỉa", "73"),
        ("KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN", "75"),
        ("    1. Kết quả đạt được", "75"),
        ("    2. Hướng phát triển", "76"),
        ("TÀI LIỆU THAM KHẢO", "77"),
        ("PHỤ LỤC", "79")
    ]
    for item, page in toc_data:
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.2
        p.paragraph_format.space_after = Pt(3)
        r1 = p.add_run(item)
        r1.font.name = 'Times New Roman'
        r1.font.size = Pt(12)
        if not item.startswith("    "):
            r1.bold = True
        
        # Leader dots
        p.paragraph_format.tab_stops.add_tab_stop(Cm(16.0))
        r2 = p.add_run(f"\t{page}")
        r2.font.name = 'Times New Roman'
        r2.font.size = Pt(12)
        if not item.startswith("    "):
            r2.bold = True

    doc.add_page_break()

    # =========================================================================
    # 6. DANH MỤC HÌNH ẢNH
    # =========================================================================
    add_h1("DANH MỤC HÌNH ẢNH")
    figs = [
        ("Hình 1. Mô hình cửa sổ suy giảm (Damped Sliding Window) theo trục thời gian", "18"),
        ("Hình 2. Cấu trúc danh sách Global DHO-List một lần quét trong bộ nhớ RAM", "21"),
        ("Hình 3. Cây tìm kiếm đệ quy DFS và cơ chế cắt tỉa an toàn bằng cận trên DUBO", "20"),
        ("Hình 4. Biểu đồ so sánh thời gian thực thi (Runtime) theo các mức ngưỡng suy giảm", "22"),
        ("Hình 5. So sánh bộ nhớ tiêu thụ Peak Heap giữa thuật toán truyền thống và DHOPM", "23"),
        ("Hình 6. Hiệu quả cắt tỉa nhánh của cận trên DUBO trên các tập dữ liệu", "24"),
        ("Hình 7. Sơ đồ kiến trúc phân tầng MVC kết hợp hệ thống 9 Mẫu thiết kế phần mềm", "29"),
        ("Hình 8. Sơ đồ Use Case tổng quát của hệ thống DHOPM Stream Visualizer", "32"),
        ("Hình 9. Sơ đồ Use Case chi tiết cho tác nhân Nhà nghiên cứu / Giảng viên", "33"),
        ("Hình 10. Sơ đồ Use Case chi tiết cho tác nhân Kỹ sư dữ liệu / Phân tích viên", "34"),
        ("Hình 11. Sơ đồ lớp tổng thể của hệ thống (Class Diagram)", "31"),
        ("Hình 12. Cấu trúc tổ chức mã nguồn dự án theo chuẩn Apache Maven", "53"),
        ("Hình 13. Giao diện Tab 1 & Tab 2: Khai phá luồng, Cây DHO-Tree và Bảng kết quả (Support tx/%)", "58"),
        ("Hình 14. Giao diện Tab 3: Khảo sát Benchmark Sweep đa ngưỡng và Biểu đồ bài báo (Fig 6, 11, 13)", "60"),
        ("Hình 15. Giao diện Tab 4: Trung tâm khai phá Big Data Tson/SPMF với tiến trình thời gian thực và ETA", "62"),
        ("Hình 16. Giao diện Tab 5: Lịch sử khai phá (Memento) và đa biểu đồ so sánh xu hướng", "64"),
        ("Hình 17. Giao diện Tab 6: Nhật ký tính toán chi tiết từng bước (Calculation Logger Inspector)", "66"),
        ("Hình 18. Hộp thoại cảnh báo an toàn bùng nổ tổ hợp đối với tập dữ liệu dày đặc", "68"),
        ("Hình 19. Kết quả thực thi kiểm thử tự động 48/48 test cases thành công trên Maven Surefire", "70")
    ]
    for fig_text, page in figs:
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.2
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.tab_stops.add_tab_stop(Cm(16.0))
        r1 = p.add_run(fig_text)
        r1.font.name = 'Times New Roman'
        r1.font.size = Pt(12)
        r2 = p.add_run(f"\t{page}")
        r2.font.name = 'Times New Roman'
        r2.font.size = Pt(12)

    doc.add_page_break()

    # =========================================================================
    # 7. DANH MỤC BẢNG BIỂU
    # =========================================================================
    add_h1("DANH MỤC BẢNG BIỂU")
    tbls = [
        ("Bảng 1. Bảng dữ liệu chuẩn 8 giao dịch và trọng số suy giảm theo thời gian (f = 0.9)", "19"),
        ("Bảng 2. Ma trận tổng hợp 9 Mẫu thiết kế phần mềm (Design Patterns) áp dụng trong hệ thống", "30"),
        ("Bảng 3. Bảng đặc tả Use case Cấu hình tham số khai phá (f, minSup)", "35"),
        ("Bảng 4. Bảng đặc tả Use case Nạp luồng dữ liệu giao dịch", "35"),
        ("Bảng 5. Bảng đặc tả Use case Thực thi khai phá luồng dữ liệu DHOPM", "36"),
        ("Bảng 6. Bảng đặc tả Use case Cắt tỉa nhánh cây tìm kiếm DFS bằng cận trên DUBO", "36"),
        ("Bảng 7. Bảng đặc tả Use case Trực quan hóa cấu trúc danh sách Global DHO-List", "37"),
        ("Bảng 8. Bảng đặc tả Use case Hiển thị kết quả mẫu khai phá (Support tx / %)", "38"),
        ("Bảng 9. Bảng đặc tả Use case Khảo sát đối sánh Benchmark Sweep đa ngưỡng minSup", "39"),
        ("Bảng 10. Bảng đặc tả Use case Dựng biểu đồ xu hướng theo bài báo gốc (Figures 6, 11, 13)", "40"),
        ("Bảng 11. Bảng đặc tả Use case Khai phá Big Data qua Tson / SPMF Engine", "41"),
        ("Bảng 12. Bảng đặc tả Use case Dừng an toàn tác vụ khai phá (Thread Interruption)", "42"),
        ("Bảng 13. Bảng đặc tả Use case Ước lượng thời gian hoàn thành (ETA Estimation)", "42"),
        ("Bảng 14. Bảng đặc tả Use case Cảnh báo an toàn bùng nổ dữ liệu dày (Dense Warning)", "43"),
        ("Bảng 15. Bảng đặc tả Use case Lưu trữ snapshot khai phá (Memento Pattern)", "44"),
        ("Bảng 16. Bảng đặc tả Use case Xem lại và phục hồi lịch sử khai phá", "44"),
        ("Bảng 17. Bảng đặc tả Use case Xóa lịch sử khai phá", "45"),
        ("Bảng 18. Bảng đặc tả Use case Ghi vết chi tiết từng bước tính toán (Calculation Log)", "46"),
        ("Bảng 19. Bảng đặc tả Use case Lọc và tra cứu nhật ký tính toán", "47"),
        ("Bảng 20. Bảng đặc tả Use case Trích xuất và sao chép công thức toán học", "47"),
        ("Bảng 21. Cấu trúc dữ liệu thực thể PatternResult", "48"),
        ("Bảng 22. Cấu trúc dữ liệu thực thể DHOEntry", "49"),
        ("Bảng 23. Cấu trúc dữ liệu thực thể MiningRunMemento", "49"),
        ("Bảng 24. Cấu trúc dữ liệu thực thể MinSupSweepResult", "50"),
        ("Bảng 25. Cấu trúc dữ liệu thực thể CalculationLogEntry", "51"),
        ("Bảng 26. Cấu trúc dữ liệu thực thể Transaction", "51"),
        ("Bảng 27. Tổng hợp các bộ dữ liệu thực nghiệm chuẩn bài báo", "69"),
        ("Bảng 28. Ma trận đối soát kiểm thử Golden Test Cases (TC1 – TC8)", "72"),
        ("Bảng 29. Bảng phân công trách nhiệm và khối lượng công việc", "79")
    ]
    for tbl_text, page in tbls:
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.2
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.tab_stops.add_tab_stop(Cm(16.0))
        r1 = p.add_run(tbl_text)
        r1.font.name = 'Times New Roman'
        r1.font.size = Pt(12)
        r2 = p.add_run(f"\t{page}")
        r2.font.name = 'Times New Roman'
        r2.font.size = Pt(12)

    doc.add_page_break()

    # =========================================================================
    # 8. MỞ ĐẦU
    # =========================================================================
    add_h1("MỞ ĐẦU")
    add_p("Trong bối cảnh kỷ nguyên số và sự phát triển vượt bậc của các nền tảng thương mại điện tử, mạng xã hội và hệ thống cảm biến thông minh, dữ liệu giao dịch được sản sinh liên tục với tốc độ chóng mặt dưới dạng các luồng dữ liệu thời gian thực (Data Streams). Việc phát hiện các tri thức ẩn, các mẫu mua sắm có giá trị chiến lược từ những dòng dữ liệu vô tận này đã trở thành nhân tố quyết định năng lực cạnh tranh và sự tồn vong của các doanh nghiệp hiện đại.", space_before=6)
    add_p("Phương pháp Khai phá tập mục phổ biến (Frequent Itemset Mining - FIM) truyền thống vốn chỉ căn cứ trên tần số xuất hiện nhị phân (0 hoặc 1) mà bỏ qua hoàn toàn quy mô của từng giỏ hàng. Điều này dẫn tới hai nghịch lý lớn: hoặc bỏ sót các tổ hợp mục có ý nghĩa chiếm tỷ trọng áp đảo trong các giỏ hàng nhỏ gọn, hoặc sinh ra hàng triệu mẫu 'loãng' ngẫu nhiên từ các hóa đơn quá dài. Để giải quyết nghịch lý này, bài toán Khai phá mẫu độ chiếm dụng cao (High Occupancy Pattern Mining - HOPM) đã ra đời, đo lường chính xác tỷ lệ số lượng món hàng trên kích thước từng giao dịch.")
    add_p("Tuy nhiên, khi đối mặt với luồng dữ liệu, HOPM gặp phải 3 thách thức kỹ thuật cốt lõi: dữ liệu chỉ được phép đọc một lần duy nhất (One-scan constraint) do giới hạn bộ nhớ RAM; dữ liệu quá khứ giảm dần giá trị theo thời gian (hiện tượng Trôi dạt khái niệm - Concept Drift); và đặc tính phi đơn điệu của độ đo Occupancy khiến không thể áp dụng các cơ chế cắt tỉa nhánh cổ điển.")
    add_p("Nhằm giải quyết trọn vẹn bài toán hóc búa trên, bài báo khoa học quốc tế 'Damped window based high occupancy pattern mining with one scanning of data streams' được công bố trên tạp chí Engineering Applications of Artificial Intelligence (EAAI, Volume 174, 2026) đã đề xuất thuật toán đột phá DHOPM. Thuật toán tích hợp mô hình suy giảm Damped Window với cận trên toán học an toàn DUBO và cấu trúc danh sách Global DHO-List một lần quét.")
    add_p("Xuất phát từ ý nghĩa học thuật tiên phong và giá trị ứng dụng thực tiễn to lớn đó, nhóm sinh viên chúng em thực hiện đề tài: 'Nghiên cứu thuật toán DHOPM và xây dựng ứng dụng trực quan hóa khai phá mẫu độ chiếm dụng cao trên luồng dữ liệu'. Đồ án không chỉ dừng lại ở việc chứng minh và làm chủ các công thức toán học, mà còn tập trung xây dựng phần mềm DHOPM Stream Visualizer hoàn chỉnh trên Java 21 và JavaFX, áp dụng 9 mẫu thiết kế phần mềm GoF chuẩn công nghiệp, mang lại một công cụ trực quan hóa sống động phục vụ nghiên cứu và giảng dạy đại học.")

    doc.add_page_break()

    # =========================================================================
    # 9. CHƯƠNG 1: TỔNG QUAN VỀ ĐỀ TÀI
    # =========================================================================
    add_h1("CHƯƠNG 1. TỔNG QUAN VỀ ĐỀ TÀI")

    add_h2("1.1. Giới thiệu đề tài")
    add_p("Khai phá dữ liệu (Data Mining) là tiến trình tự động khám phá các quy luật, tri thức tiềm ẩn và mối tương quan có ý nghĩa từ các kho dữ liệu lớn. Trong đó, khai phá mẫu (Pattern Mining) là nhánh nghiên cứu kinh điển, đóng vai trò nền móng cho các hệ thống phân tích giỏ hàng, gợi ý sản phẩm và dự báo hành vi người tiêu dùng.")
    add_p("Đề tài tập trung nghiên cứu giải thuật DHOPM — kỹ thuật khai phá các mẫu mục chiếm dụng cao trên luồng dữ liệu với mô hình suy giảm trọng số theo thời gian, đồng thời hiện thực hóa thành phần mềm máy tính DHOPM Stream Visualizer với giao diện đồ họa trực quan và tương tác mạnh mẽ.")

    add_h2("1.2. Lý do chọn đề tài")
    add_p("1. Tính vượt trội của độ đo Occupancy so với Support truyền thống: Định lượng rõ tỷ lệ phần trăm món hàng trong hóa đơn, phân biệt được giao dịch có chủ đích với giao dịch mua sắm ngẫu nhiên.")
    add_p("2. Đáp ứng hoàn hảo ràng buộc khắt khe của luồng dữ liệu lớn: Thuật toán chỉ duyệt dữ liệu đúng một lần (One-scan), giúp tiết kiệm hơn 78.5% bộ nhớ RAM so với các phương pháp lưu trữ lịch sử truyền thống.")
    add_p("3. Tính học thuật và thời sự quốc tế: Công trình được xuất bản năm 2026 trên tạp chí Q1 ISI hàng đầu (EAAI, Elsevier). Việc nghiên cứu bài báo giúp sinh viên tiếp cận chuẩn mực nghiên cứu quốc tế đỉnh cao.")
    add_p("4. Ý nghĩa thực tiễn của công cụ Visualizer: Tạo ra giải pháp trực quan hóa đồ họa cao cấp 6 Tab, phục vụ giảng dạy môn Lập trình Java Nâng cao và Khai phá Dữ liệu tại Trường Đại học Đà Lạt.")

    add_h2("1.3. Mục tiêu đề tài")
    add_p("- Mục tiêu học thuật: Chứng minh tính đúng đắn của độ đo Damped Occupancy (DO), tính đơn điệu suy giảm của cận trên DUBO và nguyên lý bảo toàn không bỏ sót mẫu kết quả.")
    add_p("- Mục tiêu sản phẩm: Xây dựng ứng dụng JavaFX 6 Tab trực quan hóa toàn diện: Cây DHO-Tree, Bảng kết quả (Support tx / %), Khảo sát Benchmark Sweep đa ngưỡng dựng đồ thị bài báo (Fig 6, 11, 13), Tson/SPMF Engine hỗ trợ Big Data với nút Dừng và ETA, Memento Pattern lưu lịch sử, và Nhật ký tính toán chi tiết (Calculation Logger).")
    add_p("- Mục tiêu kỹ thuật: Áp dụng 9 Mẫu thiết kế phần mềm GoF, phân tầng Clean MVC, kiểm thử tự động đạt 100% (48/48 unit tests PASS).")

    add_h2("1.4. Phạm vi nghiên cứu")
    add_p("- Lý thuyết: Mô hình cửa sổ suy giảm theo hàm mũ f^(TL - Td) với 0 < f <= 1 trên luồng giao dịch nhị phân.")
    add_p("- Công nghệ: Java 21 LTS, JavaFX 21, Apache Maven 3.9, SPMF Data Mining Library trên macOS/Windows/Linux.")
    add_p("- Dữ liệu: Bộ dữ liệu chuẩn default.dat (8 giao dịch) và các bộ dữ liệu lớn FIMI (retail, chess, mushroom, connect).")

    # =========================================================================
    # 10. CHƯƠNG 2: CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ
    # =========================================================================
    add_h1("CHƯƠNG 2. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ")

    add_h2("2.1. Bài toán khai phá mẫu độ chiếm dụng cao (HOPM)")
    add_p("Cho tập các mục I = {i1, i2, ..., im}. Một giao dịch Td = <TID, items> là tập con của I. Độ chiếm dụng của mẫu X trong giao dịch Td được xác định:")
    add_p("    O(X, Td) = |X| / |Td|   (nếu X là tập con của Td), và bằng 0 nếu ngược lại.")
    add_p("Tổng độ chiếm dụng trên cơ sở dữ liệu DB là tổng O(X, Td) trên toàn bộ các giao dịch chứa X. Mẫu X được gọi là Mẫu độ chiếm dụng cao (HOP) nếu O(X) >= minOcc.")

    add_h2("2.2. Khai phá trên luồng dữ liệu và mô hình Damped Window")
    add_p("Trên luồng dữ liệu T1, T2, ..., TL, mô hình Damped Window áp dụng hàm suy giảm theo cấp số nhân: Weight(Td) = f^(TL - Td) với 0 < f <= 1. Giao dịch hiện tại TL có trọng số lớn nhất là 1.0, các giao dịch quá khứ suy giảm dần theo hàm mũ tiệm cận về 0.")

    add_h2("2.3. Cơ sở toán học thuật toán DHOPM")
    add_h3("2.3.1. Độ đo Damped Occupancy (DO)")
    add_p("Độ chiếm dụng suy giảm của mẫu X tại thời điểm TL được tính bằng công thức:")
    add_p("    DO(X) = Tổng [ (|X| / |Td|) * f^(TL - Td) ] với mọi Td chứa X.")
    add_p("Mẫu X là DHOP nếu DO(X) >= minSup (với minSup = ∂ * |DB|).")

    add_h3("2.3.2. Cận trên DUBO và tính chất cắt tỉa an toàn")
    add_p("Do DO(X) không có tính đơn điệu, bài báo đề xuất cận trên Damped Upper Bound Occupancy (DUBO):")
    add_p("    DUBO(X) = Tổng [ f^(TL - Td) ] với mọi Td chứa X.")
    add_p("Vì |X| / |Td| <= 1.0, ta luôn có DO(X) <= DUBO(X). Ngoài ra, DUBO có tính chất chống đơn điệu hoàn hảo: nếu X là tập con của Y thì DUBO(Y) <= DUBO(X).")
    add_p("Định lý cắt tỉa: Nếu DUBO(X) < minSup, thì mọi tập cha Y chứa X đều có DO(Y) <= DUBO(Y) <= DUBO(X) < minSup. Do đó có thể cắt tỉa an toàn toàn bộ nhánh con của X trong cây duyệt DFS mà không làm sót bất kỳ mẫu kết quả nào.")

    add_h3("2.3.3. Cấu trúc danh sách Global DHO-List một lần quét")
    add_p("Thuật toán chỉ đọc luồng dữ liệu một lần duy nhất. Với mỗi mục, thông tin được lưu dưới dạng danh sách tuple: Entry = <TID, |Td|>. Sau khi nạp 8 giao dịch chuẩn, danh sách được sắp xếp theo Support tăng dần: G < B < A < C < D < E < F.")

    # Bảng 1
    add_p("Bảng 1. Bảng dữ liệu chuẩn 8 giao dịch và trọng số suy giảm (f = 0.9)", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Batch", "TID", "Tập các mục (Items)", "Độ dài |T|", "Trọng số f^(8 - TID)"],
        [
            ["DB0", "T1", "A, C, D, E", "4", "0.9^7 ≈ 0.4783"],
            ["DB0", "T2", "A, E, F", "3", "0.9^6 ≈ 0.5314"],
            ["DB0", "T3", "B, C, D, E", "4", "0.9^5 ≈ 0.5905"],
            ["DB0", "T4", "C, D, F", "3", "0.9^4 ≈ 0.6561"],
            ["DB1", "T5", "B, F", "2", "0.9^3 = 0.7290"],
            ["DB1", "T6", "D, E, F", "3", "0.9^2 = 0.8100"],
            ["DB2", "T7", "A, B, C, F", "4", "0.9^1 = 0.9000"],
            ["DB2", "T8", "A, E, G", "3", "0.9^0 = 1.0000"]
        ],
        [2.0, 1.8, 5.0, 2.5, 4.5]
    )

    add_h2("2.4. Công nghệ sử dụng")
    add_p("2.4.1. Ngôn ngữ lập trình Java 21 / 25: Nền tảng OOP mạnh mẽ, hỗ trợ Java Record bất biến, Pattern Matching, Garbage Collector tối ưu cho xử lý dữ liệu lớn.")
    add_p("2.4.2. Framework JavaFX 21: Tách biệt FXML và CSS, điều khiển reactive qua ObservableList, hỗ trợ đa luồng Task/Service và Charting phong phú.")
    add_p("2.4.3. Apache Maven 3.9: Quản lý thư viện, tự động hóa build lifecycle, tích hợp javafx-maven-plugin và surefire kiểm thử.")
    add_p("2.4.4. Thư viện SPMF và Core Engine: Thư viện khai phá mã nguồn mở chuẩn quốc tế, cung cấp thuật toán tối ưu hóa cho tập dữ liệu FIMI lớn.")
    add_p("2.4.5. Git và GitHub: Quản lý phiên bản phân tán, lưu trữ tại https://github.com/2312741-sudo/javanc.git.")

    # =========================================================================
    # 11. CHƯƠNG 3: PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG
    # =========================================================================
    add_h1("CHƯƠNG 3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG")

    add_h2("3.1. Phân tích yêu cầu")
    add_p("3.1.1. Yêu cầu chức năng: Cấu hình tham số f và minSup (hỗ trợ nhập bàn phím hai chiều); nạp luồng giao dịch; mô phỏng khai phá 3 pha; trực quan hóa DHO-List; hiển thị bảng mẫu có Support (tx / %); quét Benchmark Sweep đa ngưỡng minSup; khai phá Big Data Tson/SPMF với nút Dừng và ETA; cảnh báo dữ liệu dày; lưu trữ lịch sử bằng Memento; và ghi nhật ký tính toán chi tiết.")
    add_p("3.1.2. Yêu cầu phi chức năng: Tốc độ phản hồi UI < 100ms; không gây đơ giật UI nhờ đa luồng ngầm; bảo vệ an toàn bộ nhớ OOM; độ chính xác toán học 100% khớp bài báo; giao diện hiện đại và tính module hóa cao.")

    add_h2("3.2. Thiết kế hệ thống")
    add_h3("3.2.1. Kiến trúc phân tầng MVC và 9 Mẫu thiết kế GoF")
    add_p("Hệ thống áp dụng mô hình MVC kết hợp 9 Mẫu thiết kế phần mềm chuẩn công nghiệp:")

    # Bảng 2
    add_p("Bảng 2. Ma trận tổng hợp 9 Mẫu thiết kế phần mềm (Design Patterns) áp dụng trong hệ thống", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["STT", "Tên Mẫu Thiết Kế", "Phân Loại", "Lớp Triển Khai", "Mục Đích Kỹ Thuật"],
        [
            ["1", "Bridge Pattern", "Structural", "BridgeEngine", "Tách trừu tượng điều khiển UI khỏi động cơ khai phá."],
            ["2", "Adapter Pattern", "Structural", "TamSimulationBridge, TsonV1Bridge", "Chuyển đổi giao diện các động cơ về chuẩn BridgeEngine."],
            ["3", "Strategy Pattern", "Behavioral", "EngineMode", "Đóng gói các chiến lược khai phá có thể hoán đổi tại runtime."],
            ["4", "Factory Method", "Creational", "EngineFactory", "Khởi tạo động cơ phù hợp theo chế độ và tham số f, ∂."],
            ["5", "Observer Pattern", "Behavioral", "MiningListener, PhaseListener", "Phát và nhận sự kiện bất đồng bộ giữa Core Engine và UI."],
            ["6", "Memento Pattern", "Behavioral", "MiningRunMemento, HistoryManager", "Lưu snapshot từng phiên khai phá để khôi phục và so sánh."],
            ["7", "Singleton Pattern", "Creational", "MiningHistoryManager, CalcLogger", "Đảm bảo duy nhất một thể hiện quản lý lịch sử và nhật ký."],
            ["8", "MVC Pattern", "Architectural", "PatternResult, main.fxml, MainController", "Tách biệt Model dữ liệu, View giao diện và Controller."],
            ["9", "Template Method", "Behavioral", "DHOPMEngine", "Định nghĩa bộ khung 3 pha bất biến: Construct -> Reconstruct -> DFS."]
        ],
        [1.2, 3.2, 2.5, 4.0, 5.0]
    )

    add_h3("3.2.2. Sơ đồ Use Case của hệ thống")
    add_p("Hệ thống phục vụ 2 tác nhân chính: Nhà nghiên cứu / Giảng viên (tập trung vào cấu trúc DHO-Tree, công thức toán học và biểu đồ đối sánh) và Kỹ sư dữ liệu (tập trung vào khai phá dữ liệu lớn, kiểm soát tiến trình, an toàn bộ nhớ và quản lý lịch sử).")

    add_h3("3.2.3. Các bảng đặc tả Use Case chi tiết")
    use_cases = [
        ("Bảng 3. Bảng đặc tả Use case Cấu hình tham số khai phá (f, minSup)",
         "Cấu hình tham số khai phá",
         "Người dùng thiết lập hệ số suy giảm f và tỷ lệ ngưỡng ∂ qua Slider hoặc gõ trực tiếp từ bàn phím.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Người dùng chỉnh Slider hoặc nhập vào ô TextField.\n2) Hệ thống kiểm tra định dạng và đồng bộ hai chiều.\n3) Hệ thống tính toán và hiển thị minSup tuyệt đối tương ứng.",
         "Nhập sai định dạng -> Giữ nguyên giá trị hợp lệ gần nhất và báo đỏ."),

        ("Bảng 4. Bảng đặc tả Use case Nạp luồng dữ liệu giao dịch",
         "Nạp luồng dữ liệu giao dịch",
         "Đọc dữ liệu từ file chuẩn hoặc file .dat tùy chọn từ ổ đĩa.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Người dùng chọn nguồn dữ liệu và giới hạn số dòng (limit).\n2) Hệ thống phân tích cú pháp file, trích xuất danh sách mục và độ dài.\n3) Cập nhật tổng số giao dịch N lên giao diện.",
         "Tệp bị lỗi định dạng -> Hiển thị thông báo lỗi và quay về bộ dữ liệu mặc định."),

        ("Bảng 5. Bảng đặc tả Use case Thực thi khai phá luồng dữ liệu DHOPM",
         "Thực thi khai phá luồng dữ liệu DHOPM",
         "Kích hoạt chu trình khai phá 3 pha theo thuật toán DHOPM trên luồng dữ liệu.",
         "Nhà nghiên cứu",
         "1) Người dùng nhấn nút 'Bắt đầu Khai phá Stream'.\n2) Khởi tạo DHOPMEngine qua Factory Method.\n3) Thực thi tuần tự 3 pha: Construct -> Reconstruct -> DFS Mining.\n4) Cập nhật bảng mẫu và KPI.",
         "Bộ nhớ không đủ -> Giải phóng tài nguyên và đưa ra thông báo trạng thái."),

        ("Bảng 6. Bảng đặc tả Use case Cắt tỉa nhánh cây tìm kiếm DFS bằng cận trên DUBO",
         "Cắt tỉa nhánh cây tìm kiếm DFS bằng cận trên DUBO",
         "Tính cận trên DUBO của nút hiện tại và dừng tìm kiếm nhánh con nếu DUBO < minSup.",
         "Hệ thống (Tự động)",
         "1) Tính DUBO(X) cho mẫu ứng viên X.\n2) Nếu DUBO(X) < minSup, lập tức cắt tỉa toàn bộ cây con gốc X.\n3) Nếu DUBO(X) >= minSup, kiểm tra DO(X) để xác định DHOP và tiếp tục đệ quy.",
         "Không có."),

        ("Bảng 7. Bảng đặc tả Use case Trực quan hóa cấu trúc danh sách Global DHO-List",
         "Trực quan hóa cấu trúc danh sách Global DHO-List",
         "Hiển thị danh sách các mục sau khi sắp xếp tăng dần theo Support dưới dạng thẻ màu động.",
         "Nhà nghiên cứu",
         "1) Người dùng mở Tab 1 'Mô Phỏng Cây DHO-Tree'.\n2) Hệ thống sinh các đối tượng DHONodeCard theo thứ tự Support.\n3) Tô màu trạng thái: Xanh (DHOP), Đỏ (Cắt tỉa), Xám (Trung gian).",
         "Chưa có dữ liệu -> Hiển thị thông báo yêu cầu nạp dữ liệu."),

        ("Bảng 8. Bảng đặc tả Use case Hiển thị kết quả mẫu khai phá (Support tx / %)",
         "Hiển thị kết quả mẫu khai phá (Support tx / %)",
         "Trình bày bảng mẫu DHOP kèm giá trị Support hiển thị cả số lượng tx và tỷ lệ phần trăm %.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Mở Tab 2 hoặc Tab 4.\n2) Cột Support hiển thị định dạng 'X tx (Y.Y%)'.\n3) Hỗ trợ sắp xếp số học tự nhiên khi click vào tiêu đề cột.",
         "Không tìm thấy mẫu -> Hiển thị bảng trống với thông báo thích hợp."),

        ("Bảng 9. Bảng đặc tả Use case Khảo sát đối sánh Benchmark Sweep đa ngưỡng minSup",
         "Khảo sát đối sánh Benchmark Sweep đa ngưỡng minSup",
         "Tự động chạy vòng lặp qua nhiều mốc minSup để thu thập thống kê hiệu năng.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Chọn dải quét (mặc định 5% -> 30%, bước 5%).\n2) Bấm 'Chạy Quét Từng minSup'.\n3) Tác vụ nền chạy qua từng mốc, ghi nhận Runtime, Heap RAM, số DHOP và tỷ lệ cắt tỉa.\n4) Hiển thị lên bảng tblSweepResults.",
         "Bấm 'Dừng Quét' -> Dừng an toàn tại mốc hiện tại."),

        ("Bảng 10. Bảng đặc tả Use case Dựng biểu đồ xu hướng theo bài báo gốc (Figures 6, 11, 13)",
         "Dựng biểu đồ xu hướng theo bài báo gốc",
         "Vẽ các biểu đồ đường đa biến với trục hoành là minSup, tái hiện kết quả bài báo EAAI 2026.",
         "Nhà nghiên cứu",
         "1) Dựng Fig 11: Runtime giảm theo hàm mũ khi minSup tăng.\n2) Dựng Fig 6: So sánh số DHOP, mẫu bị cắt tỉa và tổng ứng viên DFS.\n3) Dựng Fig 13: Tiêu thụ bộ nhớ RAM Peak Heap duy trì thấp.",
         "Chưa có dữ liệu sweep -> Biểu đồ ở trạng thái chờ."),

        ("Bảng 11. Bảng đặc tả Use case Khai phá Big Data qua Tson / SPMF Engine",
         "Khai phá Big Data qua Tson / SPMF Engine",
         "Chạy thuật toán tối ưu hóa trên các tập dữ liệu lớn hàng trăm ngàn dòng.",
         "Kỹ sư dữ liệu",
         "1) Mở Tab 4 'Khai Phá Big Data'.\n2) Chọn chế độ (Mine, Inspect, Golden Test).\n3) Nhấn nút thực thi và theo dõi tiến trình thời gian thực.",
         "Tập dữ liệu quá dày -> Kích hoạt cảnh báo an toàn bùng nổ tổ hợp."),

        ("Bảng 12. Bảng đặc tả Use case Dừng an toàn tác vụ khai phá (Thread Interruption)",
         "Dừng an toàn tác vụ khai phá (Thread Interruption)",
         "Hủy ngang tiến trình khai phá đang chạy ngầm mà không làm rò rỉ tài nguyên hay đơ máy.",
         "Kỹ sư dữ liệu, Nhà nghiên cứu",
         "1) Nhấn nút 'Dừng Khai Phá'.\n2) Phát tín hiệu cancel() qua BridgeEngine.\n3) Luồng tính toán kiểm tra isInterrupted(), giải phóng bộ nhớ và dừng an toàn.",
         "Tiến trình đã hoàn thành -> Bỏ qua lệnh dừng."),

        ("Bảng 13. Bảng đặc tả Use case Ước lượng thời gian hoàn thành (ETA Estimation)",
         "Ước lượng thời gian hoàn thành (ETA Estimation)",
         "Tính toán và hiển thị thời gian còn lại dự kiến của tiến trình dựa trên tốc độ thực tế.",
         "Kỹ sư dữ liệu",
         "1) Đo lường tiến độ duyệt và thời gian trôi qua.\n2) Ngoại suy tuyến tính thời gian còn lại.\n3) Hiển thị thông số ETA trực quan dạng 'Còn lại: 00:15s'.",
         "Giai đoạn khởi động -> Hiển thị 'Đang ước tính...'."),

        ("Bảng 14. Bảng đặc tả Use case Cảnh báo an toàn bùng nổ dữ liệu dày (Dense Warning)",
         "Cảnh báo an toàn bùng nổ dữ liệu dày",
         "Cảnh báo nguy cơ tràn RAM khi chạy tập dữ liệu dày đặc với ngưỡng ∂ quá nhỏ.",
         "Hệ thống (Tự động)",
         "1) Phát hiện tập dữ liệu dày (chess, connect) với ∂ < 0.20.\n2) Hiển thị Alert cảnh báo người dùng.\n3) Người dùng chọn tiếp tục hoặc điều chỉnh lại ngưỡng an toàn.",
         "Người dùng hủy thao tác -> Dừng tiến trình khai phá."),

        ("Bảng 15. Bảng đặc tả Use case Lưu trữ snapshot khai phá (Memento Pattern)",
         "Lưu trữ snapshot khai phá (Memento Pattern)",
         "Đóng gói toàn bộ trạng thái phiên khai phá thành MiningRunMemento và lưu vào lịch sử.",
         "Hệ thống (Tự động)",
         "1) Thu thập thời gian, f, ∂, số giao dịch, số DHOP, pruned count, runtime, heap và top patterns.\n2) Khởi tạo MiningRunMemento.\n3) Chuyển giao cho MiningHistoryManager lưu trữ.",
         "Không có."),

        ("Bảng 16. Bảng đặc tả Use case Xem lại và phục hồi lịch sử khai phá",
         "Xem lại và phục hồi lịch sử khai phá",
         "Chọn một dòng trong bảng lịch sử để xem lại chi tiết và đồng bộ lên bảng kết quả.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Mở Tab 5 và click chọn phiên khai phá.\n2) Cập nhật lại activeTotalTransactions theo phiên đó.\n3) Đổ lại danh sách mẫu vào Tab 2 và Tab 4 với Support % chính xác.",
         "Phiên rỗng -> Hiển thị bảng trống."),

        ("Bảng 17. Bảng đặc tả Use case Xóa lịch sử khai phá",
         "Xóa lịch sử khai phá",
         "Xóa sạch toàn bộ danh sách các phiên khai phá đã lưu trong bộ nhớ tạm.",
         "Nhà nghiên cứu, Kỹ sư dữ liệu",
         "1) Nhấn nút 'Xóa Lịch Sử'.\n2) Gọi MiningHistoryManager.getInstance().clearHistory().\n3) Làm mới bảng và các biểu đồ xu hướng.",
         "Hủy xác nhận -> Giữ nguyên lịch sử."),

        ("Bảng 18. Bảng đặc tả Use case Ghi vết chi tiết từng bước tính toán (Calculation Log)",
         "Ghi vết chi tiết từng bước tính toán",
         "Ghi nhận chi tiết từng bước số học trong quá trình tính DO, DUBO và quyết định cắt tỉa.",
         "Hệ thống (Tự động)",
         "1) Gọi CalculationLogger.getInstance().log(...).\n2) Ghi nhận: ID, Pha, Đối tượng, Công thức số học, So sánh ngưỡng, Quyết định.\n3) Đổ dữ liệu vào bảng tblCalculationLogs.",
         "Vượt quá 10,000 dòng -> Tự động cắt tỉa dòng cũ để bảo vệ RAM."),

        ("Bảng 19. Bảng đặc tả Use case Lọc và tra cứu nhật ký tính toán",
         "Lọc và tra cứu nhật ký tính toán",
         "Tìm kiếm nhanh các phép tính liên quan đến một mẫu cụ thể hoặc lọc theo pha.",
         "Nhà nghiên cứu",
         "1) Nhập từ khóa tìm kiếm (ví dụ: 'AE', 'F').\n2) Chọn pha cần lọc từ ComboBox.\n3) Bảng nhật ký tức thời lọc và hiển thị các dòng thỏa mãn.",
         "Không tìm thấy -> Bảng rỗng."),

        ("Bảng 20. Bảng đặc tả Use case Trích xuất và sao chép công thức toán học",
         "Trích xuất và sao chép công thức toán học",
         "Sao chép chi tiết phép tính toán học từ nhật ký vào Clipboard để phục vụ báo cáo.",
         "Nhà nghiên cứu, Giảng viên",
         "1) Chọn dòng log cần trích xuất.\n2) Nhấn nút 'Sao Chép Log' hoặc xem vùng Formula Inspector.\n3) Chuỗi công thức được sao chép vào bộ nhớ tạm hệ điều hành.",
         "Chưa chọn dòng nào -> Nhắc người dùng chọn một dòng.")
    ]

    for title, uc_name, desc, actor, main_flow, alt_flow in use_cases:
        add_p(title, italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
        create_table(
            ["Thuộc tính", "Chi tiết đặc tả Use case"],
            [
                ["Tên Use case", uc_name],
                ["Mô tả", desc],
                ["Tác nhân chính", actor],
                ["Luồng chính", main_flow],
                ["Luồng ngoại lệ", alt_flow]
            ],
            [3.5, 12.0]
        )

    add_h3("3.2.4. Thiết kế cấu trúc dữ liệu Model")
    # Bảng 21 - 26
    add_p("Bảng 21. Cấu trúc dữ liệu thực thể PatternResult", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["pattern", "String", "Chuỗi biểu diễn tập mục (Ví dụ: '{A, E}')."],
            ["doValue", "double", "Giá trị độ đo Damped Occupancy (DO) tính toán được."],
            ["duboValue", "double", "Giá trị cận trên Damped Upper Bound Occupancy (DUBO)."],
            ["support", "int", "Số lượng giao dịch chứa mẫu mục này."],
            ["tids", "List<Integer>", "Danh sách định danh giao dịch xuất hiện mẫu."],
            ["isDHOP", "boolean", "Đánh dấu mẫu đạt chuẩn DO >= minSup hay không."],
            ["isPruned", "boolean", "Đánh dấu mẫu bị cắt tỉa bởi DUBO < minSup hay không."]
        ],
        [3.0, 3.0, 9.5]
    )

    add_p("Bảng 22. Cấu trúc dữ liệu thực thể DHOEntry", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["tid", "int", "Định danh giao dịch trong luồng (1 .. L)."],
            ["tlen", "int", "Tổng số lượng mục trong giao dịch (|Td|)."]
        ],
        [3.0, 3.0, 9.5]
    )

    add_p("Bảng 23. Cấu trúc dữ liệu thực thể MiningRunMemento", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["runId", "int", "Mã số định danh lần chạy tự tăng."],
            ["timestamp", "String", "Dấu thời gian thực thi (yyyy-MM-dd HH:mm:ss)."],
            ["datasetName", "String", "Tên tệp dữ liệu đã nạp."],
            ["totalTransactions", "long", "Tổng số giao dịch của tệp dữ liệu."],
            ["f", "double", "Hệ số suy giảm thời gian (0 < f <= 1)."],
            ["partial", "double", "Tỷ lệ ngưỡng tối thiểu (∂)."],
            ["minSup", "double", "Giá trị ngưỡng tuyệt đối tính toán được."],
            ["dhopCount", "int", "Tổng số lượng mẫu DHOP tìm được."],
            ["prunedCount", "int", "Số lượng nhánh con bị cắt tỉa bởi DUBO."],
            ["runtimeMs", "long", "Thời gian thực thi thuật toán (ms)."],
            ["peakHeapMb", "double", "Dung lượng bộ nhớ RAM Heap tiêu thụ (MB)."],
            ["topPatterns", "List<PatternResult>", "Danh sách toàn bộ các mẫu kết quả của lần chạy."]
        ],
        [3.5, 3.2, 8.8]
    )

    add_p("Bảng 24. Cấu trúc dữ liệu thực thể MinSupSweepResult (Java Record)", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["partial", "double", "Mốc tỷ lệ ngưỡng ∂ đang khảo sát."],
            ["minSup", "double", "Giá trị ngưỡng tuyệt đối minSup tương ứng."],
            ["dhopCount", "int", "Số lượng mẫu DHOP đạt chuẩn tại mốc này."],
            ["prunedCount", "int", "Số lượng mẫu bị cắt tỉa bởi DUBO."],
            ["totalCandidates", "int", "Tổng số lượng mẫu ứng viên đã duyệt trong DFS."],
            ["runtimeMs", "long", "Thời gian chạy tại mốc này (ms)."],
            ["peakHeapMb", "double", "Mức tiêu thụ bộ nhớ RAM Heap tại mốc này (MB)."]
        ],
        [3.5, 3.0, 9.0]
    )

    add_p("Bảng 25. Cấu trúc dữ liệu thực thể CalculationLogEntry (Java Record)", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["id", "int", "Số thứ tự phép tính tự tăng."],
            ["phase", "String", "Pha thực thi thuật toán (Pha 1, Pha 2, Pha 3)."],
            ["target", "String", "Mục hoặc mẫu mục đang được tính toán."],
            ["formula", "String", "Chi tiết công thức số học và các bước triển khai."],
            ["comparison", "String", "Biểu thức so sánh với ngưỡng minSup."],
            ["decision", "String", "Quyết định của thuật toán (DHOP, CẮT TỈA, MỞ RỘNG)."]
        ],
        [3.0, 3.0, 9.5]
    )

    add_p("Bảng 26. Cấu trúc dữ liệu thực thể Transaction", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Trường", "Kiểu dữ liệu", "Mô tả ý nghĩa"],
        [
            ["tid", "int", "Định danh thứ tự giao dịch trong luồng."],
            ["items", "List<String>", "Tập hợp các mục sản phẩm chứa trong giao dịch."]
        ],
        [3.0, 3.0, 9.5]
    )

    # =========================================================================
    # 12. CHƯƠNG 4: XÂY DỰNG HỆ THỐNG VÀ HIỆN THỰC HÓA
    # =========================================================================
    add_h1("CHƯƠNG 4. XÂY DỰNG HỆ THỐNG VÀ HIỆN THỰC HÓA")

    add_h2("4.1. Cấu trúc thư mục và tổ chức mã nguồn")
    add_p("Mã nguồn tuân thủ Clean Architecture của Maven, phân tầng rõ rệt: vn.edu.dlu.dhopm.core (thuật toán lõi), .bridge (mẫu Adapter & Bridge), .history (mẫu Memento & Singleton), .log (mẫu Observer & Singleton), .ui.controller (bộ điều khiển JavaFX), và .view (tệp main.fxml cùng style.css).")

    add_h2("4.2. Hiện thực hóa các phân hệ lõi nghiệp vụ")
    add_p("Thuật toán lõi DHOPMEngine cài đặt theo mẫu Template Method, tuân thủ 3 pha bất biến:")
    add_p("    - Pha 1 (Construct): Đọc luồng dữ liệu 1 lần, ghi nhận cặp <TID, |Td|> vào DHO-List.")
    add_p("    - Pha 2 (Reconstruct): Sắp xếp thứ tự các mục theo Support tăng dần, tính DO và DUBO.")
    add_p("    - Pha 3 (DFS Mining): Duyệt cây đệ quy theo chiều sâu, cắt tỉa bằng cận trên DUBO và thu thập các mẫu DHOP.")
    add_p("Lớp MinSupSweepService tự động chạy dải ngưỡng ∂ từ 0.05 đến 0.50, đo lường thời gian chạy (System.nanoTime()) và bộ nhớ Heap RAM đã sử dụng.")
    add_p("Lớp MiningHistoryManager (Singleton & Caretaker) lưu trữ snapshot các lần chạy bằng MiningRunMemento, hỗ trợ phục hồi bảng kết quả kèm tính toán tỷ lệ % Support chuẩn xác.")

    add_h2("4.3. Xây dựng giao diện trực quan hóa JavaFX")
    add_p("4.3.1. Tab 1 & Tab 2: Hiển thị thẻ card DHONodeCard động đổi màu theo trạng thái; Bảng kết quả hiển thị Support định dạng 'X tx (Y.Y%)' với bộ so sánh tự nhiên Comparator.comparingInt(...).")
    add_p("4.3.2. Tab 3: Khảo sát Benchmark Sweep đa ngưỡng minSup, dựng 3 biểu đồ đường chuẩn bài báo EAAI 2026: Fig 11 (Runtime vs minSup), Fig 6 (DHOPs & Cắt tỉa vs minSup), Fig 13 (Peak Heap RAM vs minSup), và bảng kết quả đối chiếu chi tiết.")
    add_p("4.3.3. Tab 4: Trung tâm khai phá Big Data Tson/SPMF với thanh tiến trình % thời gian thực, nút Dừng an toàn (Thread Interruption) và ước tính ETA.")
    add_p("4.3.4. Tab 5: Lịch sử khai phá (Memento) và các biểu đồ so sánh xu hướng Runtime, Heap RAM và Tỷ lệ cắt tỉa giữa các lượt chạy.")
    add_p("4.3.5. Tab 6: Nhật ký tính toán chi tiết từng bước (Calculation Logger) với thanh lọc theo pha, tìm kiếm từ khóa mẫu, và vùng hiển thị Formula Inspector.")

    add_h2("4.4. Xử lý đa luồng bất đồng bộ và kiểm soát an toàn bộ nhớ")
    add_p("Mọi tác vụ tính toán nặng được đóng gói trong Task<T> chạy trên Worker Thread ngầm. Cập nhật giao diện thông qua Platform.runLater(). Tích hợp cảnh báo bùng nổ dữ liệu dày đặc khi ∂ < 0.20 đối với các tệp như chess.dat, tránh nguy cơ treo ứng dụng.")

    # =========================================================================
    # 13. CHƯƠNG 5: KẾT QUẢ THỰC NGHIỆM VÀ ĐỐI SOÁT BÀI BÁO GỐC
    # =========================================================================
    add_h1("CHƯƠNG 5. KẾT QUẢ THỰC NGHIỆM VÀ ĐỐI SOÁT BÀI BÁO GỐC")

    add_h2("5.1. Bộ dữ liệu thực nghiệm")
    add_p("Bảng 27. Tổng hợp các bộ dữ liệu thực nghiệm chuẩn bài báo", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Tên bộ dữ liệu", "Số giao dịch (|DB|)", "Số lượng mục", "Độ dài TB", "Đặc tính phân bố"],
        [
            ["default.dat", "8", "7", "3.25", "Dữ liệu mẫu bài báo EAAI 2026"],
            ["retail.dat", "88,162", "16,470", "10.30", "Dữ liệu siêu thị thực tế, mật độ thưa"],
            ["mushroom.dat", "8,124", "119", "23.00", "Dữ liệu sinh học nấm, mật độ dày"],
            ["chess.dat", "3,196", "75", "37.00", "Dữ liệu nước cờ vua, mật độ rất dày"],
            ["connect.dat", "67,557", "129", "43.00", "Dữ liệu trò chơi Connect-4, cực kỳ dày đặc"]
        ],
        [3.0, 3.0, 2.5, 2.5, 4.5]
    )

    add_h2("5.2. Kết quả kiểm thử tự động toàn diện (48 Test Cases)")
    add_p("Bộ kiểm thử tự động toàn diện trên nền tảng JUnit 5 đạt kết quả tuyệt đối 48/48 ca kiểm thử thành công (100% BUILD SUCCESS):")
    add_p("- Lab1VerificationTest (8 tests): Khớp kết quả tính tay Lab 1 (sai số < 10^-4).")
    add_p("- DHOPMEngineTest (10 tests): Kiểm thử 3 pha và thuật toán DFS.")
    add_p("- MinSupSweepServiceTest (2 tests): Kiểm thử quét benchmark đa ngưỡng.")
    add_p("- TsonBridgeIntegrationTest (6 tests): Đối soát 2 động cơ trên dữ liệu chuẩn.")
    add_p("- BridgeEngineTest (10 tests): Kiểm thử hợp đồng Bridge và chuyển đổi động cơ.")
    add_p("- MiningProgressInfoTest (3 tests): Kiểm thử định dạng tiến trình và ETA.")
    add_p("- TsonToolsServiceTest (4 tests): Kiểm thử dịch vụ thống kê.")
    add_p("- MiningHistoryManagerTest (3 tests): Kiểm thử Memento Pattern.")
    add_p("- CalculationLoggerTest (2 tests): Kiểm thử ghi nhật ký và trích xuất công thức.")

    add_h2("5.3. Đối soát tính đúng đắn với công trình gốc (Golden Tests)")
    add_p("Bảng 28. Ma trận đối soát kiểm thử Golden Test Cases (TC1 – TC8)", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Mã TC", "Hệ số f", "Ngưỡng ∂", "Ngưỡng minSup", "Kỳ vọng bài báo", "Kết quả phần mềm", "Đánh giá"],
        [
            ["TC1", "0.90", "15%", "1.20", "2 mẫu: {AE}, {F}", "2 mẫu: {AE}, {F}", "PASS (100%)"],
            ["TC2", "0.90", "20%", "1.60", "0 mẫu (Tập rỗng)", "0 mẫu (Tập rỗng)", "PASS (100%)"],
            ["TC3", "0.90", "10%", "0.80", "6 mẫu: {AE, F, BE...}", "6 mẫu: Trùng khớp 100%", "PASS (100%)"],
            ["TC4", "0.95", "15%", "1.20", "4 mẫu", "4 mẫu: Trùng khớp 100%", "PASS (100%)"],
            ["TC5", "1.00", "15%", "1.20", "9 mẫu: {AE, F, CD...}", "9 mẫu: Trùng khớp 100%", "PASS (100%)"],
            ["TC6", "1.00", "20%", "1.60", "4 mẫu: {AE, F, CD, DE}", "4 mẫu: Trùng khớp 100%", "PASS (100%)"],
            ["TC7", "0.80", "15%", "1.20", "1 mẫu: {AE}", "1 mẫu: Trùng khớp 100%", "PASS (100%)"],
            ["TC8", "0.85", "15%", "1.20", "2 mẫu: {AE}, {F}", "2 mẫu: Trùng khớp 100%", "PASS (100%)"]
        ],
        [1.5, 1.8, 1.8, 2.2, 4.0, 4.0, 2.2]
    )

    add_h2("5.4. Đánh giá hiệu năng và hiệu quả cắt tỉa")
    add_p("1. Thời gian chạy: Trên tập retail.dat (88,162 dòng), thời gian chạy dao động từ 78ms đến 185ms. Mức giảm thời gian khi tăng ∂ đạt trên 57.8%, hoàn toàn khớp với quy luật hàm mũ trong Figure 4 của bài báo gốc.")
    add_p("2. Bộ nhớ RAM Heap: Duy trì ổn định dưới 45 MB trên retail.dat, giảm 78.5% so với lưu trữ chuỗi giao dịch.")
    add_p("3. Tỷ lệ cắt tỉa nhánh DFS: Cận trên DUBO cắt tỉa từ 85% đến 95% không gian tìm kiếm, ngăn chặn bùng nổ tổ hợp thành công.")

    # =========================================================================
    # 14. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN
    # =========================================================================
    add_h1("KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN")

    add_h2("1. Kết quả đạt được")
    add_p("1. Làm chủ lý thuyết học thuật: Chứng minh và cài đặt hoàn chỉnh thuật toán DHOPM từ bài báo EAAI 2026 với mô hình Damped Window, độ đo DO, cận trên DUBO và cấu trúc nén DHO-List một lần quét.")
    add_p("2. Kiến trúc chuẩn mực: Ứng dụng thành công 9 Mẫu thiết kế phần mềm GoF, phân tầng Clean MVC và nguyên lý SOLID.")
    add_p("3. Ứng dụng Visualizer toàn diện: Xây dựng thành công phần mềm DHOPM Stream Visualizer bằng JavaFX 21 gồm 6 Tab chức năng, hỗ trợ Support (tx / %), quét Benchmark Sweep tái hiện biểu đồ bài báo, nút Dừng an toàn, ước tính ETA, quản lý lịch sử (Memento) và nhật ký tính toán chi tiết.")
    add_p("4. Kiểm thử chất lượng cao: Vượt qua 100% bộ 48 ca kiểm thử tự động JUnit 5, đối soát khớp hoàn toàn với số liệu bài báo gốc.")

    add_h2("2. Hướng phát triển")
    add_p("1. Nâng cấp BitSet DFS Engine: Tối ưu hóa biểu diễn bằng mảng bit và phép toán bitwise AND cấp phần cứng để tăng tốc độ khai phá lên 5 - 10 lần.")
    add_p("2. Tích hợp Message Broker: Kết nối Apache Kafka / RabbitMQ để thu nhận luồng dữ liệu giao dịch trực tiếp từ các sàn thương mại điện tử thực tế.")
    add_p("3. Mở rộng trên nền tảng phân tán: Triển khai trên Apache Flink / Spark Streaming để xử lý hàng triệu giao dịch mỗi giây trên môi trường Cloud.")

    # =========================================================================
    # 15. TÀI LIỆU THAM KHẢO
    # =========================================================================
    add_h1("TÀI LIỆU THAM KHẢO")
    add_p("[1] M. Cho, H. Kim, P. Fournier-Viger, and U. Yun, 'Damped window based high occupancy pattern mining with one scanning of data streams', Engineering Applications of Artificial Intelligence, vol. 174, p. 114511, 2026. DOI: 10.1016/j.engappai.2026.114511.")
    add_p("[2] R. Agrawal, T. Imieliński, and A. Swami, 'Mining association rules between sets of items in large databases', in Proc. of ACM SIGMOD, pp. 207-216, 1993.")
    add_p("[3] J. Han, J. Pei, and Y. Yin, 'Mining frequent patterns without candidate generation', ACM SIGMOD Record, vol. 29, no. 2, pp. 1-12, 2000.")
    add_p("[4] E. Gamma, R. Helm, R. Johnson, and J. Vlissides, 'Design Patterns: Elements of Reusable Object-Oriented Software', Addison-Wesley, 1994.")
    add_p("[5] B. Goetz, T. Peierls, J. Bloch, J. Bowbeer, D. Holmes, and D. Lea, 'Java Concurrency in Practice', Addison-Wesley, 2006.")
    add_p("[6] OpenJFX Documentation, 'JavaFX 21: Client Application Platform', https://openjfx.io, 2024.")
    add_p("[7] P. Fournier-Viger et al., 'The SPMF Open-Source Data Mining Library', Journal of Machine Learning Research (JMLR), vol. 17, no. 1, pp. 1-5, 2016.")
    add_p("[8] FIMI Repository, 'Frequent Itemset Mining Implementations Repository', http://fimi.uantwerpen.be/data/, 2004.")

    # =========================================================================
    # 16. PHỤ LỤC
    # =========================================================================
    add_h1("PHỤ LỤC")
    add_h2("Bảng phân công trách nhiệm và khối lượng công việc")
    add_p("Bảng 29. Bảng phân công trách nhiệm và khối lượng công việc", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
    create_table(
        ["Họ và Tên", "Vai trò", "Nội dung công việc đảm nhiệm", "Mức độ hoàn thành"],
        [
            ["Nguyễn Thanh Tâm\n(MSSV: 2312741)", "Trưởng nhóm",
             "- Phân tích kiến trúc hệ thống MVC và áp dụng 9 Mẫu thiết kế GoF.\n- Thiết kế và lập trình toàn bộ giao diện JavaFX 6 Tab (main.fxml, style.css, MainController.java).\n- Cài đặt phân hệ Benchmark Sweep đa ngưỡng và dựng hệ thống đồ thị đối sánh (Fig 6, 11, 13).\n- Hiện thực Memento Pattern và Observer Pattern cho nhật ký tính toán.\n- Xây dựng bộ 48 ca kiểm thử JUnit 5 và viết báo cáo học thuật hoàn chỉnh.", "100%"],
            ["Nguyễn Hữu Trung Sơn", "Thành viên",
             "- Nghiên cứu cơ sở toán học thuật toán DHOPM và cấu trúc Global DHO-List một lần quét.\n- Hiện thực hóa thuật toán lõi DHOPMEngine và cơ chế cắt tỉa an toàn bằng cận trên DUBO.\n- Đóng gói và tích hợp động cơ Tson / SPMF Engine xử lý dữ liệu lớn.\n- Thực hiện các ca kiểm thử Golden Test Cases (TC1 – TC8).", "100%"]
        ],
        [3.5, 2.5, 8.5, 2.5]
    )

    add_h2("Hướng dẫn cài đặt, biên dịch và vận hành ứng dụng")
    add_p("1. Yêu cầu môi trường: Cài đặt JDK 17+ (khuyến nghị JDK 21 LTS) và Apache Maven 3.8+.")
    add_p("2. Biên dịch và kiểm thử tự động:")
    add_p("    mvn clean test")
    add_p("   Hệ thống thực thi 48 ca kiểm thử đơn vị với kết quả 100% BUILD SUCCESS.")
    add_p("3. Khởi chạy ứng dụng đồ họa JavaFX:")
    add_p("    ./run.sh    hoặc    mvn javafx:run")
    add_p("   Cửa sổ ứng dụng DHOPM Stream Visualizer sẽ hiển thị với đầy đủ 6 Tab trực quan hóa.")

    # Save document
    output_path = "/Users/nthtam/Lưu trữ/javanangcao/dhopm-visualizer/docs/BaoCao_DeTai_DHOPM_2312741_NguyenThanhTam.docx"
    doc.save(output_path)
    print(f"Báo cáo Word đã được tạo thành công tại: {output_path}")

if __name__ == "__main__":
    build_report()
