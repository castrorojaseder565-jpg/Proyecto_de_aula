from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from pathlib import Path

OUT=Path(__file__).parent/'Informe_Investigacion_Operaciones_BoviControl.docx'
BLUE='24577A'; NAVY='18364D'; GREEN='47724A'; LIGHT='F2F5F6'; WHITE='FFFFFF'; TEXT='29343A'; MUTED='64747C'

def shade(cell,color):
    pr=cell._tc.get_or_add_tcPr(); el=OxmlElement('w:shd'); el.set(qn('w:fill'),color); pr.append(el)
def cell(cell,text,bold=False,color=TEXT,size=9):
    cell.text=''; p=cell.paragraphs[0]; p.paragraph_format.space_after=Pt(1); p.paragraph_format.line_spacing=1.0
    r=p.add_run(str(text)); r.bold=bold; r.font.name='Arial'; r.font.size=Pt(size); r.font.color.rgb=RGBColor.from_string(color)
    cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
def table(doc,heads,rows,widths=None,size=8.7):
    t=doc.add_table(rows=1,cols=len(heads)); t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.style='Table Grid'
    for i,h in enumerate(heads): shade(t.rows[0].cells[i],BLUE); cell(t.rows[0].cells[i],h,True,WHITE,size)
    for k,row in enumerate(rows):
        cs=t.add_row().cells
        for i,val in enumerate(row):
            if k%2: shade(cs[i],LIGHT)
            cell(cs[i],val,size=size)
    if widths:
        for row in t.rows:
            for i,w in enumerate(widths): row.cells[i].width=Inches(w)
    doc.add_paragraph().paragraph_format.space_after=Pt(0)
    return t
def para(doc,txt='',bold=False,size=10.2,color=TEXT,align=None):
    p=doc.add_paragraph(); p.paragraph_format.space_after=Pt(5); p.paragraph_format.line_spacing=1.08
    if align is not None:p.alignment=align
    r=p.add_run(txt); r.bold=bold; r.font.name='Arial'; r.font.size=Pt(size); r.font.color.rgb=RGBColor.from_string(color)
    return p
def heading(doc,txt,level=1):
    p=doc.add_paragraph(); p.paragraph_format.keep_with_next=True; p.paragraph_format.space_before=Pt(8 if level==1 else 5); p.paragraph_format.space_after=Pt(4)
    r=p.add_run(txt); r.bold=True; r.font.name='Arial'; r.font.size=Pt(15 if level==1 else 11.5); r.font.color.rgb=RGBColor.from_string(NAVY if level==1 else BLUE)

doc=Document(); sec=doc.sections[0]
sec.top_margin=Inches(.62); sec.bottom_margin=Inches(.62); sec.left_margin=Inches(.72); sec.right_margin=Inches(.72)
doc.styles['Normal'].font.name='Arial'; doc.styles['Normal'].font.size=Pt(10.2)
h=sec.header.paragraphs[0]; h.alignment=WD_ALIGN_PARAGRAPH.RIGHT
r=h.add_run('BOVICONTROL  |  INVESTIGACIÓN DE OPERACIONES'); r.bold=True; r.font.name='Arial'; r.font.size=Pt(8); r.font.color.rgb=RGBColor.from_string(GREEN)
f=sec.footer.paragraphs[0]; f.alignment=WD_ALIGN_PARAGRAPH.CENTER
r=f.add_run('Proyecto de aula · 7 de octubre de 2026 · '); r.font.size=Pt(8); r.font.color.rgb=RGBColor.from_string(MUTED)
field=OxmlElement('w:fldSimple'); field.set(qn('w:instr'),'PAGE'); f._p.append(field)

# Página 1
para(doc,'Investigación de Operaciones',True,23,NAVY,WD_ALIGN_PARAGRAPH.CENTER)
para(doc,'Asignación de veterinarios a atenciones de vacunación en BoviControl',True,16,GREEN,WD_ALIGN_PARAGRAPH.CENTER)
para(doc,'Eder Castro · Mateo Garrido · Andrea Galeano',False,10.5,TEXT,WD_ALIGN_PARAGRAPH.CENTER)
para(doc,'Ingeniería de Sistemas · VII semestre · [Institución por completar] · Octubre de 2026',False,9,MUTED,WD_ALIGN_PARAGRAPH.CENTER)
heading(doc,'1. Planteamiento del problema')
para(doc,'BoviControl registra bovinos, vacunas aplicadas y fechas de próxima dosis. Cuando existen varias dosis próximas o vencidas, la atención debe organizarse con un número limitado de veterinarios y con jornadas de duración finita. La aplicación actual permite consultar la información sanitaria, pero el modelo de datos revisado no registra una asignación de cada atención a un veterinario ni su disponibilidad por turno.')
para(doc,'En ausencia de un criterio común, la programación puede no reflejar la urgencia de las dosis ni la carga que puede asumir cada profesional. Se propone un modelo que asigne las atenciones candidatas a veterinarios disponibles, favoreciendo las de mayor prioridad y respetando tanto las horas como el máximo de atenciones por jornada.')
para(doc,'Problema a resolver. Dado un conjunto de atenciones de vacunación pendientes y los veterinarios disponibles para una jornada, decidir quién atenderá cada caso para maximizar la prioridad sanitaria cubierta sin exceder la capacidad individual.',True)
heading(doc,'1.1 Alcance y supuestos')
para(doc,'Cada atención representa una combinación validada de animal y vacuna que requiere una acción dentro del horizonte de planeación. La elegibilidad y la prioridad deben ser confirmadas por un profesional veterinario. El modelo apoya la programación y no determina diagnósticos, tratamientos ni protocolos clínicos.')
para(doc,'La solución de este informe es una formulación académica propuesta. El código existente no contiene todavía disponibilidad por turno, estimaciones de duración ni una relación de asignación entre vacunas pendientes y veterinarios.')
doc.add_page_break()

# Página 2
heading(doc,'2. Modelo matemático')
para(doc,'El problema se formula como una asignación generalizada con capacidades mediante programación lineal entera binaria. Se asigna a lo sumo un veterinario a cada atención y se limitan las horas y el número de atenciones por profesional.')
heading(doc,'2.1 Conjuntos y parámetros',2)
table(doc,['Símbolo','Significado'],[
 ('i ∈ I, i = 1,…,n','Atenciones de vacunación candidatas (animal + vacuna) en el horizonte.'),
 ('j ∈ J, j = 1,…,m','Veterinarios que podrían participar en la jornada.'),
 ('aⱼ','Disponibilidad del veterinario j: 1 si está en turno; 0 si no está disponible.'),
 ('wᵢ','Días de atraso de la atención i respecto a su fecha próxima de dosis. Si aún no vence, wᵢ = 0.'),
 ('pᵢ','Prioridad sanitaria de i. Propuesta inicial: 1 pendiente; 2 próxima a aplicar; 3 vencida. Debe validarla el veterinario.'),
 ('dᵢ','Duración estimada, en minutos, para atender la atención i.'),
 ('Tⱼ','Minutos de trabajo disponibles del veterinario j durante la jornada.'),
 ('Cⱼ','Máximo de atenciones que puede recibir el veterinario j en la jornada.'),
 ('bᵢⱼ','Compatibilidad: 1 si j está autorizado y puede atender i; 0 en caso contrario.')],[1.25,5.75],8.6)
para(doc,'La prioridad 1–3 es una regla inicial para modelar el problema, no una regla clínica implementada en BoviControl. Debe ajustarse con el responsable veterinario. En la entidad Vacuna se observa proxima_dosis y una clasificación calculada con fecha; la disponibilidad, duración, compatibilidad y capacidad se deben obtener fuera del sistema o incorporarse al proceso.')
heading(doc,'2.2 Variable de decisión',2)
para(doc,'xᵢⱼ = 1 si la atención i se asigna al veterinario j; 0 en caso contrario, para todo i ∈ I y j ∈ J. Se tienen n × m variables binarias.')
heading(doc,'2.3 Función objetivo',2)
para(doc,'Maximizar  Z =  Σᵢ∈I Σⱼ∈J  pᵢ · xᵢⱼ',True,13,NAVY,WD_ALIGN_PARAGRAPH.CENTER)
para(doc,'La función maximiza la suma de prioridades cubiertas. Así, si la capacidad no alcanza para todos los casos, el modelo favorece las atenciones con mayor prioridad. No se introducen costos ni diferencias de preferencia entre veterinarios porque esos datos no están disponibles en el proyecto.')
doc.add_page_break()

# Página 3
heading(doc,'2.4 Restricciones')
heading(doc,'(R1) Cada atención recibe a lo sumo un veterinario',2)
para(doc,'Σⱼ∈J xᵢⱼ ≤ 1   ∀ i ∈ I')
para(doc,'Una atención no puede duplicarse y puede quedar sin asignar si no hay capacidad suficiente.')
heading(doc,'(R2) Disponibilidad y compatibilidad',2)
para(doc,'xᵢⱼ ≤ aⱼ · bᵢⱼ   ∀ i ∈ I, j ∈ J')
para(doc,'No se asigna una atención a quien no esté de turno o no tenga compatibilidad aprobada para realizarla. La compatibilidad puede representar alcance profesional, zona o disponibilidad del insumo, si esos criterios se definen y mantienen.')
heading(doc,'(R3) Tiempo de trabajo por veterinario',2)
para(doc,'Σᵢ∈I dᵢ · xᵢⱼ ≤ Tⱼ · aⱼ   ∀ j ∈ J')
para(doc,'La suma de los minutos estimados no supera el tiempo disponible. Cuando aⱼ = 0, el lado derecho es cero y el profesional no recibe asignaciones.')
heading(doc,'(R4) Máximo de atenciones por jornada',2)
para(doc,'Σᵢ∈I xᵢⱼ ≤ Cⱼ · aⱼ   ∀ j ∈ J')
para(doc,'Limita el número de atenciones por veterinario en el bloque de planeación. Este límite representa carga por jornada, no atenciones simultáneas.')
heading(doc,'(R5) Naturaleza de las variables',2)
para(doc,'xᵢⱼ ∈ {0, 1}   ∀ i ∈ I, j ∈ J')
heading(doc,'2.5 Resumen del modelo')
table(doc,['Elemento','Definición'],[
 ('Variable','xᵢⱼ ∈ {0,1}: la atención i se asigna al veterinario j.'),
 ('Objetivo','Máx Z = ΣᵢΣⱼ pᵢ xᵢⱼ; prioriza las atenciones de mayor urgencia.'),
 ('R1','Una atención, a lo sumo un veterinario.'),
 ('R2','Solo veterinarios disponibles y compatibles.'),
 ('R3','Tiempo asignado dentro de los minutos disponibles.'),
 ('R4','Número de atenciones bajo la capacidad de jornada.'),
 ('Tipo','Programación lineal entera binaria; resoluble con Solver de Excel, PuLP u otro optimizador.')],[1.2,5.8],8.8)
para(doc,'Interpretación. Si la capacidad y la compatibilidad son suficientes, el modelo puede asignar todas las atenciones candidatas. Si no lo son, deja algunas sin asignar y maximiza la prioridad cubierta, sin exceder los límites de cada veterinario. En caso de empate entre veterinarios, este modelo no distingue cuál recibe la asignación; para balancear cargas se necesitaría un criterio adicional validado.')
doc.add_page_break()

# Página 4
heading(doc,'3. Datos del proyecto y aplicación del modelo')
table(doc,['Dato','Disponibilidad observada en BoviControl','Uso / trabajo pendiente'],[
 ('Animal y vacuna','Entidades Ganado y Vacuna; relación de vacuna con ganado.','Construir la lista de atenciones candidatas con registros autorizados.'),
 ('Fecha de próxima dosis','Vacuna.proxima_dosis; puede no estar definida.','Validar datos faltantes y definir la fecha de corte.'),
 ('Estado','Vacuna dispone de estado y estado calculado según próxima dosis.','Acordar con profesional la prioridad pᵢ y la elegibilidad.'),
 ('Veterinario asignado','Usuario registra rol, pero Vacuna no guarda responsable asignado.','Crear/usar proceso para vincular atención con veterinario.'),
 ('Turno aⱼ y minutos Tⱼ','No se observan campos de turno ni disponibilidad horaria.','Ingresar calendario real aprobado por coordinación.'),
 ('Duración dᵢ y capacidad Cⱼ','No están registradas en las entidades revisadas.','Estimar con observaciones de operación y validación del equipo.'),
 ('Compatibilidad bᵢⱼ','No está representada en el modelo actual.','Definir si se requiere por zona, permiso o tipo de vacuna.')],[1.35,2.75,2.9],8.1)
heading(doc,'3.1 Método de solución propuesto',2)
para(doc,'1. Extraer el listado de dosis candidatas desde registros autorizados y fijar la fecha/hora de corte.\n2. Validar con el veterinario cuáles requieren atención en el horizonte y su prioridad.\n3. Obtener disponibilidad por jornada, tiempos estimados, capacidad y compatibilidades.\n4. Cargar los parámetros en un solver entero y verificar que la solución cumpla R1–R5.\n5. Revisar la agenda sugerida con el profesional y documentar ajustes, causas y fecha.')
heading(doc,'3.2 Evaluación y limitaciones',2)
para(doc,'Para una instancia real se recomienda informar: número de atenciones programadas, prioridad total cubierta, casos no asignados y motivo, minutos utilizados por veterinario y capacidad remanente. Se deben comparar escenarios de disponibilidad y duración. No se presenta un resultado numérico porque no se suministraron datos de turnos, tiempos, capacidades ni un conjunto autorizado de dosis pendientes.')
heading(doc,'4. Conclusiones')
para(doc,'La asignación binaria permite formalizar cómo distribuir las atenciones de vacunación entre los veterinarios disponibles, haciendo explícitas la prioridad sanitaria y las capacidades de jornada. BoviControl ya contiene información de animales y fechas de dosis que puede apoyar la preparación de las instancias, pero no almacena todos los parámetros ni la asignación resultante.')
para(doc,'Para llevar el modelo a operación se requiere validar prioridades, elegibilidad y tiempos con personal veterinario, acordar la disponibilidad real y diseñar dónde persistir la asignación. Hasta que esas actividades se completen y se ejecute un solver con datos, el modelo debe considerarse una propuesta académica, no una función desplegada del sistema.')
heading(doc,'Referencias')
para(doc,'BoviControl. Código fuente local revisado: entidades Ganado y Vacuna, SecurityConfig y servicios de gestión sanitaria. Octubre de 2026.',False,9)
para(doc,'Taha, H. A. (2017). Operations Research: An Introduction (10th ed.). Pearson.',False,9)
para(doc,'Nota: completar institución, docente y URL del repositorio antes de entregar. Los integrantes deben revisar y sustentar la formulación.',False,9,MUTED)

doc.core_properties.title='Investigación de Operaciones — Asignación de veterinarios a atenciones de vacunación en BoviControl'
doc.core_properties.author='Eder Castro; Mateo Garrido; Andrea Galeano'
doc.core_properties.subject='Modelo de asignación generalizada con capacidades'
doc.save(OUT)
print(OUT)
