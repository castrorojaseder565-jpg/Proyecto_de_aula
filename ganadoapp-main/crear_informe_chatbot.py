from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

ROOT = Path(__file__).parent
OUT = ROOT / 'Informe_Chatbot_BoviControl_Desarrollo_Web_Avanzado.docx'
BLUE = '24577A'; NAVY = '18364D'; GREEN = '47724A'; PALE = 'EAF0F3'; LIGHT = 'F4F6F7'; WHITE = 'FFFFFF'; RED = 'A63A32'

def font(size, bold=False):
    paths = [r'C:\Windows\Fonts\arial.ttf', r'C:\Windows\Fonts\arialbd.ttf']
    try: return ImageFont.truetype(paths[1 if bold else 0], size)
    except: return ImageFont.load_default()

def diagram():
    w,h=1500,790; im=Image.new('RGB',(w,h),'white'); d=ImageDraw.Draw(im)
    def box(x,y,bw,bh,title,sub,color=BLUE):
        d.rounded_rectangle((x,y,x+bw,y+bh),radius=20,fill='#'+PALE,outline='#'+color,width=4)
        d.text((x+20,y+18),title,font=font(29,True),fill='#'+NAVY)
        lines=sub.split('\n')
        for i,s in enumerate(lines): d.text((x+20,y+64+i*32),s,font=font(21),fill='#29343A')
    def arrow(x1,y1,x2,y2,label=''):
        d.line((x1,y1,x2,y2),fill='#'+GREEN,width=6)
        import math
        a=math.atan2(y2-y1,x2-x1); L=22
        p1=(x2-L*math.cos(a-.55),y2-L*math.sin(a-.55)); p2=(x2-L*math.cos(a+.55),y2-L*math.sin(a+.55))
        d.polygon([(x2,y2),p1,p2],fill='#'+GREEN)
        if label: d.text(((x1+x2)//2-70,(y1+y2)//2-28),label,font=font(18,True),fill='#'+NAVY)
    box(45,285,280,170,'Usuario autenticado','ADMIN / VETERINARIO')
    box(405,285,300,170,'Interfaz web','Thymeleaf: chatbot.html\nJavaScript: chatbot.js')
    box(790,285,300,170,'API Spring Boot','POST /api/chat\nValidación + CSRF')
    box(1170,100,280,190,'ChatbotService','Contexto del animal\ny vacunas desde MySQL',GREEN)
    box(1170,490,280,190,'Groq Chat Completions','Modelo Qwen 3.8\nservicio externo',BLUE)
    box(785,555,310,150,'Persistencia','MySQL\nganado 1 — N vacuna',GREEN)
    arrow(325,370,405,370,'pregunta')
    arrow(705,370,790,370,'JSON')
    arrow(1090,330,1170,230,'mensaje')
    arrow(1310,290,1310,490,'contexto')
    arrow(1170,580,1095,610,'consulta')
    arrow(1170,555,1090,420,'respuesta')
    d.text((45,40),'Arquitectura observada en el código del proyecto',font=font(34,True),fill='#'+NAVY)
    p=ROOT/'diagrama_chatbot_bovicontrol.png'; im.save(p); return p

def shade(cell,color):
    tcPr=cell._tc.get_or_add_tcPr(); shd=OxmlElement('w:shd'); shd.set(qn('w:fill'),color); tcPr.append(shd)

def celltext(cell,text,bold=False,color='263238',size=9):
    cell.text=''; p=cell.paragraphs[0]; p.paragraph_format.space_after=Pt(2)
    r=p.add_run(str(text)); r.bold=bold; r.font.name='Arial'; r.font.size=Pt(size); r.font.color.rgb=RGBColor.from_string(color)
    cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER

def table(doc, headers, rows, widths=None, size=8.5):
    t=doc.add_table(rows=1, cols=len(headers)); t.alignment=WD_TABLE_ALIGNMENT.CENTER; t.style='Table Grid'
    for i,x in enumerate(headers): shade(t.rows[0].cells[i],BLUE); celltext(t.rows[0].cells[i],x,True,WHITE,size)
    for ri,row in enumerate(rows):
        cells=t.add_row().cells
        for i,x in enumerate(row):
            if ri%2==1: shade(cells[i],LIGHT)
            celltext(cells[i],x,size=size)
    if widths:
        for row in t.rows:
            for i,w in enumerate(widths): row.cells[i].width=Inches(w)
    doc.add_paragraph().paragraph_format.space_after=Pt(1)
    return t

def p(doc,text='',bold=False,size=10.5,color='263238',align=None,style=None):
    para=doc.add_paragraph(style=style)
    if align is not None: para.alignment=align
    para.paragraph_format.space_after=Pt(6); para.paragraph_format.line_spacing=1.08
    r=para.add_run(text); r.bold=bold; r.font.name='Arial'; r.font.size=Pt(size); r.font.color.rgb=RGBColor.from_string(color)
    return para

def heading(doc,text,level=1):
    para=doc.add_paragraph(); para.paragraph_format.keep_with_next=True; para.paragraph_format.space_before=Pt(10 if level==1 else 6); para.paragraph_format.space_after=Pt(5)
    r=para.add_run(text); r.bold=True; r.font.name='Arial'; r.font.size=Pt(16 if level==1 else 12); r.font.color.rgb=RGBColor.from_string(NAVY if level==1 else BLUE)
    return para

def code(doc,lines):
    t=doc.add_table(rows=1,cols=1); t.style='Table Grid'; c=t.cell(0,0); shade(c,'F1F3F4'); c.text=''
    for i,line in enumerate(lines.splitlines()):
        para=c.paragraphs[0] if i==0 else c.add_paragraph(); para.paragraph_format.space_after=Pt(0); para.paragraph_format.line_spacing=1.0
        r=para.add_run(line); r.font.name='Consolas'; r.font.size=Pt(8); r.font.color.rgb=RGBColor.from_string('263238')
    doc.add_paragraph().paragraph_format.space_after=Pt(0)

doc=Document(); sec=doc.sections[0]; sec.top_margin=Inches(.62); sec.bottom_margin=Inches(.62); sec.left_margin=Inches(.72); sec.right_margin=Inches(.72)
normal=doc.styles['Normal']; normal.font.name='Arial'; normal.font.size=Pt(10.5); normal.font.color.rgb=RGBColor.from_string('263238')
header=sec.header.paragraphs[0]; header.alignment=WD_ALIGN_PARAGRAPH.RIGHT; rr=header.add_run('BOVICONTROL  |  DESARROLLO WEB AVANZADO'); rr.bold=True; rr.font.name='Arial'; rr.font.size=Pt(8); rr.font.color.rgb=RGBColor.from_string(GREEN)
footer=sec.footer.paragraphs[0]; footer.alignment=WD_ALIGN_PARAGRAPH.CENTER; rr=footer.add_run('Informe técnico del asistente conversacional  •  7 de octubre de 2026  •  '); rr.font.size=Pt(8); rr.font.color.rgb=RGBColor.from_string('718087')
fld=OxmlElement('w:fldSimple'); fld.set(qn('w:instr'),'PAGE'); footer._p.append(fld)

# Portada
p(doc,'BoviControl',True,34,NAVY,WD_ALIGN_PARAGRAPH.CENTER)
p(doc,'ASISTENTE CONVERSACIONAL\nPARA GESTIÓN BOVINA',True,23,GREEN,WD_ALIGN_PARAGRAPH.CENTER)
p(doc,'Informe técnico de desarrollo web avanzado',False,15,'52636C',WD_ALIGN_PARAGRAPH.CENTER)
p(doc,'',size=8)
p(doc,'Consulta contextual de animales y registros de vacunación con Spring Boot, Thymeleaf y MySQL',False,12,NAVY,WD_ALIGN_PARAGRAPH.CENTER)
p(doc,'',size=10)
table(doc,['Datos académicos','Información'],[
 ('Programa','Ingeniería de Sistemas · VII semestre'),('Integrantes','Eder Castro · Mateo Garrido · Andrea Galeano'),
 ('Institución','[Por diligenciar]'),('Docente','[Por diligenciar]'),('Repositorio','URL del repositorio del equipo: [por agregar]'),('Fecha','7 de octubre de 2026')],[2.0,4.8],10)
p(doc,'Documento elaborado a partir de la inspección del código fuente local de BoviControl. Los fragmentos describen la implementación identificada; las pruebas indicadas como pendientes no se reportan como ejecutadas.',False,9,'63727A',WD_ALIGN_PARAGRAPH.CENTER)
doc.add_page_break()

heading(doc,'1. Contexto y alcance')
p(doc,'BoviControl es una aplicación web para registrar ganado, gestionar vacunas y consultar información del hato. El sistema está construido con Java y Spring Boot; las vistas usan Thymeleaf y la persistencia relacional se configura para MySQL. Este documento describe la integración del asistente conversacional observada en el repositorio y la relaciona con los componentes existentes.')
table(doc,['Componente','Tecnología / ubicación','Estado identificado'],[
 ('Interfaz','Thymeleaf: templates/chatbot.html; static/js/chatbot.js','Vista y envío de preguntas implementados.'),
 ('Control web','ChatbotPageController','Expone GET /chatbot.'),('API','ChatController, /api/chat','POST JSON; respuesta con propiedad respuesta.'),
 ('Lógica contextual','ChatbotService','Busca animal y vacunas, forma contexto y llama a Groq Chat Completions.'),
 ('Persistencia','JPA + MySQL','Entidades Ganado y Vacuna; relación uno a muchos.'),('Acceso','Spring Security','Chat restringido a ADMIN y VETERINARIO.')],[1.25,2.7,3.1],8.5)
heading(doc,'1.1 Alcance funcional',2)
p(doc,'El asistente recibe preguntas en español y pide a Groq clasificarlas en intenciones permitidas. Antes de consultar MySQL, el backend valida el rol y ejecuta una consulta de conteo, de dosis próximas/vencidas o de un animal identificado. Después entrega solo el resultado verificado al modelo para redactar la respuesta. Los conteos generales son exclusivos de ADMIN; ADMIN y VETERINARIO pueden consultar fichas y dosis por atender.')
heading(doc,'1.2 Límites del alcance',2)
p(doc,'El código inspeccionado no implementa historial de conversaciones guardado por usuario, RAG documental, sensores, ejecución autónoma de acciones ni analítica en el chatbot. Las respuestas del proveedor requieren conectividad, una credencial válida y cuota disponible. La revisión del código no demuestra que una llamada real al proveedor haya concluido satisfactoriamente.')
img=diagram(); doc.add_picture(str(img),width=Inches(6.85)); p(doc,'Figura 1. Arquitectura del flujo identificada en el código fuente.',False,8,'63727A',WD_ALIGN_PARAGRAPH.CENTER)
doc.add_page_break()

heading(doc,'2. Problema que resuelve el asistente')
p(doc,'Consultar antecedentes de un animal implica localizar su ficha y revisar las vacunas asociadas. El asistente busca ofrecer una interacción por lenguaje natural para ese recorrido. La necesidad principal es que las respuestas sobre registros provengan del sistema y que los límites del servicio externo se comuniquen con claridad.')
table(doc,['Rol','Consulta esperada','Contexto disponible para respuesta'],[
 ('ADMIN','¿Cuántos animales tenemos registrados?','Conteo directo desde MySQL.'),
 ('ADMIN','¿Cuántos animales están vacunados?','Conteo de animales distintos con al menos una aplicación registrada.'),
 ('ADMIN o VETERINARIO','Dime cuáles necesitan vacunación esta semana.','Animales y dosis vencidas o con fecha hasta el domingo de la semana actual.'),
 ('ADMIN o VETERINARIO','¿Qué vacunas tiene el animal 125?','Datos del animal y hasta 30 vacunas consultados en MySQL.'),
 ('VETERINARIO','¿Cuántos animales tenemos registrados?','403 antes de ejecutar la consulta de conteo.'),
 ('Usuario sin identificador','¿Cuántas vacunas tiene?','No se consultan registros concretos; el asistente debe solicitar ID/arete/código.'),
 ('Registro inexistente','¿Qué vacunas tiene el animal X?','Se envía al modelo que no existe un animal con ese identificador.')],[1.25,2.6,3.2],8.5)
heading(doc,'2.1 Consideraciones de privacidad',2)
p(doc,'El contexto puede contener datos identificables del hato. El servicio transmite a Groq la pregunta y los campos seleccionados del animal y sus vacunas. Por eso, el despliegue debe usar una credencial fuera del repositorio, limitar los datos remitidos a lo indispensable, explicar el tratamiento a los usuarios y aplicar las políticas institucionales y legales pertinentes.')

heading(doc,'3. Componentes del chatbot')
heading(doc,'3.1 Responsabilidades por clase',2)
table(doc,['Clase / archivo','Capa','Responsabilidad'],[
 ('ChatbotPageController.java','Controlador MVC','Entrega la plantilla chatbot para GET /chatbot.'),
 ('ChatController.java','API REST','Valida la entrada, delega al servicio y devuelve respuesta o error.'),
 ('ChatRequest.java','DTO','mensaje obligatorio, máximo 1000 caracteres.'),
 ('ChatResponse.java / ChatErrorResponse.java','DTO','Transportan respuesta o descripción del error.'),
 ('ChatbotService.java','Servicio','Clasifica intención, valida el rol, consulta datos autorizados y entrega el contexto verificado a Groq Chat Completions.'),
 ('GanadoService / VacunaService','Dominio','Acceden a los registros del animal y vacunas.')],[2.35,1.35,3.35],8.5)
heading(doc,'3.2 Contrato de entrada y salida',2)
code(doc,'// Solicitud\n{ "mensaje": "¿Qué vacunas tiene el animal 125?" }\n\n// Respuesta satisfactoria\n{ "respuesta": "..." }\n\n// Error del asistente (HTTP 503)\n{ "error": "..." }')
heading(doc,'3.3 Construcción del contexto',2)
p(doc,'El clasificador reconoce conteo total, animales con vacunas aplicadas, dosis que requieren atención esta semana, consulta de un animal específico y preguntas generales. El backend usa COUNT(DISTINCT ganado_id) para contar animales con al menos una aplicación registrada. Para vacunación semanal consulta dosis con fecha hasta el domingo en curso e incluye las vencidas; agrupa las dosis por animal y limita el texto enviado al modelo a 50 atenciones. En consultas individuales busca por arete, código o ID y limita a 30 vacunas.')
code(doc,'// El backend valida rol y luego consulta MySQL\nlong animales = ganadoService.contarGanadosRegistrados();\nlong vacunados = vacunaService.contarAnimalesConVacunaAplicada();\n\n// Próxima fecha hasta el cierre de semana\nList<Vacuna> dosis = vacunaService\n    .obtenerQueRequierenAtencionHasta(finSemana);')
doc.add_page_break()

heading(doc,'4. Integración con el proveedor de IA')
heading(doc,'4.1 Solicitud al servicio externo',2)
p(doc,'ChatbotService usa java.net.http.HttpClient y envía POST HTTPS a Groq Chat Completions. Cada pregunta se clasifica primero con un conjunto cerrado de intenciones; después de validar permisos y consultar MySQL, se envía el contexto junto con la pregunta para redactar la respuesta. Se limita la salida a 500 tokens y la solicitud a 60 segundos.')
code(doc,'cuerpo.put("model", model);\ncuerpo.put("max_completion_tokens", maxTokens);\nmessages.addObject().put("role", "system").put("content", instrucciones);\nmessages.addObject().put("role", "user").put("content", entrada);\n\n.header("Authorization", "Bearer " + apiKey)\n.header("Content-Type", "application/json")')
heading(doc,'4.2 Manejo de errores',2)
table(doc,['Condición','Comportamiento del código'],[
 ('Sin clave configurada','Indica configurar GROQ_API_KEY; no realiza solicitud.'),('HTTP 401','Señala que Groq rechazó la clave.'),
 ('HTTP 429','Informa que se alcanzó un límite de solicitudes o tokens.'),
 ('HTTP 502/503/504','Informa indisponibilidad temporal del servicio.'),('Otro HTTP no exitoso','Devuelve error con código HTTP y recomienda revisar el modelo.'),
 ('Interrupción o I/O','Restaura la interrupción cuando corresponde y presenta mensaje de conexión/interrupción.'),
 ('Respuesta sin texto','Informa que no llegó una respuesta textual.')],[1.7,5.3],8.5)
heading(doc,'4.3 Configuración requerida',2)
p(doc,'application.properties.example muestra variables externas para la conexión MySQL. En application.properties se identificó además una clave de proveedor con un valor predeterminado que parece ser un secreto. Este informe no reproduce el valor. Debe eliminarse del archivo, invalidarse/rotarse si fue real y suministrarse únicamente mediante entorno seguro. La credencial no debe incluirse en Git, capturas ni entregas.')
p(doc,'El cliente usa el endpoint de Groq para chat completions. Su ruta incluye el segmento /openai/v1 por compatibilidad de formato; el dominio de destino es api.groq.com y no se hacen llamadas al servicio de OpenAI.')
code(doc,'# Patrón de configuración\ngroq.api-key=${GROQ_API_KEY:}\ngroq.model=${GROQ_MODEL:qwen/qwen3.8-27b}')
heading(doc,'4.4 Dependencias externas',2)
p(doc,'El endpoint depende de la disponibilidad, autorización, límites y compatibilidad del proveedor. GROQ_API_KEY no está configurada en el entorno local al preparar este informe; por tanto, la integración compila, pero falta una llamada real para confirmar la generación de respuestas. El modelo qwen/qwen3.8-27b figura como modelo preview en el catálogo de Groq y se deja configurable mediante GROQ_MODEL.')

heading(doc,'5. Interfaz web')
heading(doc,'5.1 Vista chatbot.html',2)
p(doc,'La plantilla muestra la marca BoviControl, el usuario autenticado, ejemplos de preguntas sobre conteos y vacunación semanal, un área de conversación con atributos de accesibilidad y un formulario. El texto de ayuda recuerda que los conteos generales son exclusivos de ADMIN y que las consultas individuales requieren ID, código o arete. El límite visible es 1000 caracteres, consistente con ChatRequest.')
heading(doc,'5.2 Flujo en chatbot.js',2)
table(doc,['Paso','Comportamiento'],[
 ('1. Envío','Evita el envío vacío, agrega el mensaje del usuario y limpia el campo.'),
 ('2. Solicitud','POST /api/chat con JSON, credenciales same-origin y encabezado X-CSRF-TOKEN.'),
 ('3. Resultado','Lee la respuesta JSON y agrega la respuesta del asistente.'),
 ('4. Error','Muestra el texto error devuelto por servidor o un mensaje genérico.'),
 ('5. Estado','Bloquea el campo/botón durante la llamada y los reactiva al finalizar.')],[1.2,5.8],8.5)
heading(doc,'5.3 Seguridad de presentación',2)
p(doc,'El DOM inserta el texto mediante textContent, evitando interpretarlo como HTML. El formulario transmite el token CSRF disponible en la página y conserva la cookie de autenticación same-origin. La autorización efectiva sigue dependiendo de Spring Security en backend, no solo de la interfaz.')
doc.add_page_break()

heading(doc,'6. Control de acceso y seguridad')
heading(doc,'6.1 Reglas configuradas',2)
table(doc,['Recurso/operación','Acceso identificado'],[
 ('/chatbot y /api/chat','ADMIN o VETERINARIO'),('/usuarios/**','ADMIN'),
 ('Crear/editar/eliminar ganado y registrar leche','ADMIN'),('Registrar/editar/eliminar vacunas','VETERINARIO'),
 ('Listas y detalle del ganado, predicción y gestión sanitaria','ADMIN o VETERINARIO'),
 ('Autenticación','Formulario de inicio de sesión; contraseñas con BCryptPasswordEncoder.')],[3.4,3.6],8.5)
code(doc,'.requestMatchers("/chatbot", "/api/chat")\n    .hasAnyRole("ADMIN", "VETERINARIO")')
heading(doc,'6.2 Verificaciones recomendadas',2)
table(doc,['Prueba','Resultado que debe verificarse'],[
 ('GET /chatbot sin sesión','Redirección al inicio de sesión.'),('POST /api/chat sin sesión','Solicitud rechazada por el filtro de seguridad.'),
 ('Usuario autenticado sin rol habilitado','Acceso denegado.'),('ADMIN y VETERINARIO','Acceso autorizado al endpoint.'),
 ('VETERINARIO pide conteo general','HTTP 403 y ninguna consulta de conteo.'),
 ('ADMIN pide conteo general','Respuesta basada en COUNT directo de MySQL.'),
 ('ADMIN/VETERINARIO piden dosis semanales','Consulta limitada al fin de semana y lista agrupada por animal.'),
 ('POST con mensaje vacío o >1000 caracteres','Validación de entrada rechaza la solicitud.'),
 ('Solicitud sin CSRF válido','Protección CSRF rechaza la petición POST.'),
 ('Cuota agotada o credencial inválida','Mensaje de error entendible, sin clave ni datos sensibles en respuesta/log.')],[3.0,4.0],8.5)
p(doc,'Estas son pruebas propuestas; no se afirma que hayan sido ejecutadas para este informe.',False,9,RED)

heading(doc,'6.3 Hallazgos y acciones antes de publicar',2)
table(doc,['Hallazgo','Acción'],[
 ('Credencial de IA escrita como valor predeterminado en application.properties','El valor se retiró. Configurar GROQ_API_KEY desde el entorno/secret manager y rotar la credencial anterior antes de publicar.'),
 ('Clave de Groq aún no configurada','Crear una clave de GroqCloud y añadirla a .env; sin ella no puede validarse una respuesta real.'),
 ('Pruebas de rol no evidenciadas','Ejecutar casos para anónimo, rol no autorizado, ADMIN y VETERINARIO.'),
 ('Dependencia de datos enviados al proveedor','Minimizar atributos transmitidos, definir información al usuario y política de uso.'),
 ('Repositorio público/URL no suministrados','Completar enlace del equipo y verificar historial por si contiene secretos antes de entrega.')],[2.8,4.2],8.3)

heading(doc,'7. Modelo de datos relacionado')
p(doc,'La relación central para el asistente es Ganado 1 — N Vacuna. El conteo de vacunados usa animales distintos con una fecha de aplicación registrada; no afirma que el esquema completo esté al día. La consulta semanal usa proxima_dosis y la fecha de corte en America/Bogota. La tabla usuario respalda los perfiles de la aplicación. El resumen no sustituye el esquema desplegado.')
table(doc,['Entidad','Campos observados relevantes','Relación / regla'],[
 ('ganado','id, codigo_identificacion, numero_arete, nombre, raza, peso, edad, unidad_tiempo_edad, sexo, estado_salud, propietario','Código y arete únicos; se relaciona con vacunas.'),
 ('vacuna','id, ganado_id, nombre, fecha_aplicacion, proxima_dosis, intervalo_cantidad, intervalo_meses, unidad_tiempo, estado','FK ganado_id obligatoria; relación N:1 hacia ganado.'),
 ('usuario','id, nombre, correo, password, rol','Datos usados por autenticación; contraseña debe almacenarse cifrada.')],[1.0,3.7,2.3],8.1)
heading(doc,'7.1 Diccionario de datos abreviado',2)
table(doc,['Tabla.campo','Tipo Java / SQL conceptual','Uso'],[
 ('ganado.id','Long / BIGINT','Identificador primario.'),('ganado.codigo_identificacion','String / VARCHAR(50)','Código único generado con prefijo BOV.'),
 ('ganado.numero_arete','String / VARCHAR(30)','Número de arete único.'),('ganado.estado_salud','String / VARCHAR(50)','Estado registrado del animal.'),
 ('vacuna.ganado_id','Ganado / BIGINT FK','Identifica el animal al que se asocia la vacuna.'),('vacuna.fecha_aplicacion','LocalDate / DATE','Fecha de aplicación registrada.'),
 ('vacuna.proxima_dosis','LocalDate / DATE nullable','Fecha próxima si está definida.'),('vacuna.estado','String / VARCHAR(50)','Estado registrado; el modelo también calcula estado según fecha.'),
 ('usuario.rol','String / VARCHAR','Rol de autenticación (p. ej., ADMIN/VETERINARIO).')],[2.1,2.2,2.7],8.1)
heading(doc,'7.2 Observación de consistencia',2)
p(doc,'Vacuna conserva intervalo_cantidad y una columna legado intervalo_meses sincronizada antes de persistir. El informe documenta esa compatibilidad tal como aparece en la entidad; conviene confirmar con el esquema SQL de la instalación qué columna utiliza la base activa.')
doc.add_page_break()

heading(doc,'8. Organización del repositorio y evidencia')
table(doc,['Ruta local','Contenido'],[
 ('src/main/java/.../controller/ChatController.java','Endpoint REST del chat.'),
 ('src/main/java/.../controller/ChatbotPageController.java','Controlador de página.'),
 ('src/main/java/.../service/ChatbotService.java','Contexto, integración HTTP y errores.'),
 ('src/main/java/.../web/ChatRequest.java, ChatResponse.java, ChatErrorResponse.java','Objetos de transferencia.'),
 ('src/main/java/.../config/SecurityConfig.java','Autenticación, autorización y protección de rutas.'),
 ('src/main/java/.../mode/Ganado.java, Vacuna.java, Usuario.java','Entidades del dominio.'),
 ('src/main/resources/templates/chatbot.html','Interfaz HTML Thymeleaf.'),
 ('src/main/resources/static/js/chatbot.js','Lógica del navegador.')],[3.6,3.4],8.5)
heading(doc,'8.1 Evidencia disponible y pendiente',2)
table(doc,['Evidencia','Estado'],[
 ('Inspección estática de controladores, servicio, plantilla, JavaScript, configuración de seguridad y entidades','Realizada para redactar este documento.'),
 ('Respuesta real de Groq con datos de prueba','Pendiente de configurar GROQ_API_KEY; no se ha confirmado una respuesta generada.'),
 ('Acceso por roles y CSRF con sesión','Pendiente de ejecutar en una instancia activa.'),
 ('Capturas de Postman/navegador y datos de prueba autorizados','Pendientes de adjuntar por el equipo.'),
 ('Repositorio remoto del equipo','URL no incluida en los archivos revisados.')],[4.0,3.0],8.5)
heading(doc,'9. Conclusiones del avance')
p(doc,'El repositorio contiene la página web, endpoint, clasificador de intención, validación de roles, consultas agregadas y de vacunación en MySQL, y conexión con Groq para presentar los resultados. Se implementan conteos administrativos, consulta semanal agrupada y ficha por identificador, con mensajes de error del proveedor.')
p(doc,'La evidencia disponible permite describir la implementación con Groq, pero no afirmar funcionamiento generativo validado porque aún falta configurar una clave válida y ejecutar una llamada real. Se retiró del archivo de configuración una credencial que estaba escrita como valor predeterminado; debe rotarse antes de compartir o publicar el repositorio.')
heading(doc,'9.1 Próximas actividades',2)
for s in ['Rotar la credencial anterior, configurar GROQ_API_KEY de forma segura y confirmar que el modelo elegido esté habilitado para la cuenta.','Ejecutar consultas reales con animales existentes, identificadores inexistentes y sin identificador.','Verificar permisos ADMIN/VETERINARIO, usuario anónimo, validación de 1000 caracteres y CSRF.','Registrar capturas con datos de prueba autorizados, fecha, versión de código y resultado observado.','Solicitar revisión veterinaria de exactitud y límites de las respuestas antes de usarlo en decisiones clínicas.']:
    p(doc,'• '+s,size=10)
heading(doc,'Referencias técnicas')
for s in ['Código fuente local de BoviControl, inspeccionado el 7 de octubre de 2026.','Spring Boot Reference Documentation. https://docs.spring.io/spring-boot/reference/','Spring Security Reference. https://docs.spring.io/spring-security/reference/','Groq API Reference — Chat Completions. https://console.groq.com/docs/api-reference','Groq Supported Models. https://console.groq.com/docs/models']:
    p(doc,s,size=9)
p(doc,'Nota académica: completar institución, docente, URL del repositorio y evidencia de pruebas. Revisar el documento con los integrantes antes de entregarlo.',False,9,'63727A')

doc.core_properties.title='BoviControl — Asistente conversacional: informe técnico de Desarrollo Web Avanzado'
doc.core_properties.author='Eder Castro; Mateo Garrido; Andrea Galeano'
doc.core_properties.subject='Descripción técnica basada en inspección del código fuente'
doc.save(OUT)
print(OUT)
