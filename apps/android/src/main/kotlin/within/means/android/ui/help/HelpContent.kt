package within.means.android.ui.help

/**
 * Static educational catalogue for the Help / "Aprende" view.
 *
 * Source of truth for the content: doc/within-means-app/HELP-EDUCATION-SPEC.md.
 * Modelled as plain data (rule #0 / Decisión E) so it is testable, translatable
 * and editable without touching Compose. The view renders this; it owns no copy.
 *
 * Jargon stays out of the user-facing strings (spec §6.3): titles use everyday
 * language; the technical name lives only in [Lesson.alsoKnownAs] as small print.
 */

/** A deep-link action a lesson/step can trigger, taking the user to the real screen. */
enum class HelpAction(val label: String) {
    QUICK_ADD("Registrar un movimiento"),
    STATS("Ver mis números"),
    CATEGORIES("Ver mis categorías"),
}

/**
 * Optional illustration a lesson can render to *show* a pattern, not just tell it.
 * Kept as a flag (not a Composable) so the content layer stays free of Compose.
 */
enum class LessonVisual {
    /** No illustration. */
    NONE,

    /** The 50 / 30 / 20 proportion bar (essential / choice / savings). */
    SPLIT_50_30_20,
}

/** One step of the minimal "Empieza por aquí" guided route (§4.2). */
data class GuideStep(
    val emoji: String,
    val title: String,
    val oneLiner: String,
    val action: HelpAction,
)

/** A library lesson (accordion card, §3). [inTheApp] anchors it to a real screen/data. */
data class Lesson(
    val emoji: String,
    val title: String,
    val oneLiner: String,
    val whatItIs: String,
    val whyItMatters: String,
    val inTheApp: String,
    val rule: String,
    /** A concrete worked case with round numbers (§ejemplo). Warm, no guilt. May be null. */
    val example: String? = null,
    val action: HelpAction? = null,
    /** Technical name, shown as small print only ("también llamado…"). May be null. */
    val alsoKnownAs: String? = null,
    /** Optional illustration the view renders to show the pattern. */
    val visual: LessonVisual = LessonVisual.NONE,
)

/** A titled group of lessons inside the "¿Quieres profundizar?" disclosure (§4.3). */
data class LessonLibrary(
    val title: String,
    val subtitle: String,
    val lessons: List<Lesson>,
)

/** §4.2 — the only thing the user *needs* to read. Four steps, in flow order. */
val guidedRoute: List<GuideStep> = listOf(
    GuideStep(
        emoji = "💙",
        title = "Anota lo que entra",
        oneLiner = "Cada vez que recibas dinero, regístralo como ingreso.",
        action = HelpAction.QUICK_ADD,
    ),
    GuideStep(
        emoji = "🫙",
        title = "Aparta tu ahorro primero",
        oneLiner = "Antes de gastar, separa una parte como ahorro.",
        action = HelpAction.QUICK_ADD,
    ),
    GuideStep(
        emoji = "🧾",
        title = "Anota lo que gastas",
        oneLiner = "Un toque por gasto. Con calma, sin culpa.",
        action = HelpAction.QUICK_ADD,
    ),
    GuideStep(
        emoji = "📊",
        title = "Mira cómo vas",
        oneLiner = "Tus números te cuentan a dónde va tu dinero.",
        action = HelpAction.STATS,
    ),
)

/** §6.1 — "Lee tus números". Reading the KPIs analytics already computes. */
private val readYourNumbers = LessonLibrary(
    title = "Lee tus números",
    subtitle = "Qué te dicen tus cifras, sin tecnicismos.",
    lessons = listOf(
        Lesson(
            emoji = "💰",
            title = "Cuánto te queda al mes",
            oneLiner = "Lo que entra menos lo que sale: tu ahorro del mes.",
            whatItIs = "Es la diferencia entre tus ingresos y tus gastos en el mes. Si es positiva, ahorraste; si es negativa, gastaste de más.",
            whyItMatters = "Es el número que mejor resume tu mes. Subirlo poco a poco, sin agobios, es el objetivo de todo lo demás.",
            inTheApp = "Lo ves en Análisis, como tu ahorro del mes y su evolución.",
            rule = "Una buena referencia: que te quede al menos 1 de cada 5 (un 20%).",
            example = "Entran 1.500 y gastas 1.200 → te quedan 300. Eso es 1 de cada 5: vas en buen rumbo.",
            action = HelpAction.STATS,
            alsoKnownAs = "tasa de ahorro",
        ),
        Lesson(
            emoji = "⚖️",
            title = "Lo que necesitas vs lo que eliges",
            oneLiner = "Separa lo imprescindible de lo que es decisión tuya.",
            whatItIs = "Cada gasto es esencial (vivir: comida, casa, transporte) o elección (ocio, caprichos). La app ya lo sabe por la categoría.",
            whyItMatters = "Lo que eliges es tu margen real: es lo primero que puedes ajustar en un mes flojo sin tocar lo importante.",
            inTheApp = "En Análisis, la lente que separa gastos esenciales de los discrecionales.",
            rule = "No es para recortar caprichos: es para gastarlos sabiendo que puedes.",
            action = HelpAction.STATS,
            alsoKnownAs = "esencial vs discrecional",
        ),
        Lesson(
            emoji = "🔁",
            title = "Gastos que se repiten vs los que cambian",
            oneLiner = "Cuánto de tu dinero ya está comprometido cada mes.",
            whatItIs = "Hay gastos fijos (alquiler, suscripciones) que se repiten igual, y variables (super, ocio) que cambian mes a mes.",
            whyItMatters = "Si casi todo es fijo, tienes poco margen ante un imprevisto. Bajar los fijos te da aire y tranquilidad.",
            inTheApp = "La app distingue fijo y variable por la categoría de cada gasto.",
            rule = "Revisa tus fijos cada cierto tiempo: una suscripción olvidada es dinero que se va sin que lo decidas.",
            action = HelpAction.CATEGORIES,
            alsoKnownAs = "gasto fijo vs variable",
        ),
        Lesson(
            emoji = "🌱",
            title = "Gastos que te devuelven algo",
            oneLiner = "No todo gasto es una fuga: algunos son inversión en ti.",
            whatItIs = "Salud, formación o herramientas son gastos que generan valor a futuro, no solo se consumen.",
            whyItMatters = "Separarlos te quita la culpa de gastar en lo que te hace crecer, y te ayuda a no recortar justo eso.",
            inTheApp = "Marca esas categorías como productivas al crearlas o editarlas.",
            rule = "Antes de recortar un gasto, pregúntate: ¿esto me devuelve algo?",
            action = HelpAction.CATEGORIES,
            alsoKnownAs = "gasto productivo",
        ),
    ),
)

/** §6.2 — "Técnicas para mover mejor el dinero". */
private val techniques = LessonLibrary(
    title = "Técnicas para mover mejor el dinero",
    subtitle = "Hábitos sencillos que cambian el rumbo.",
    lessons = listOf(
        Lesson(
            emoji = "🫙",
            title = "Aparta tu ahorro primero",
            oneLiner = "Ahorra al principio del mes, no con lo que sobre.",
            whatItIs = "Apenas recibes tu ingreso, separas tu ahorro como un movimiento más, antes de empezar a gastar.",
            whyItMatters = "Lo que se aparta primero se conserva. Lo que se deja “para lo que sobre”, casi siempre se gasta.",
            inTheApp = "Regístralo como Ahorro en el registro rápido en cuanto entre tu dinero.",
            rule = "Si puedes, automatízalo como movimiento recurrente.",
            example = "Cobras hoy: antes de nada, apartas 150 como Ahorro. El resto ya es tu mes; gástalo con calma.",
            action = HelpAction.QUICK_ADD,
            alsoKnownAs = "págate a ti primero",
        ),
        Lesson(
            emoji = "🥧",
            title = "El reparto 50 / 30 / 20",
            oneLiner = "La mitad para lo necesario, algo para gustos, algo para ti.",
            whatItIs = "Una guía simple: ~50% a lo esencial, ~30% a lo que eliges y ~20% a ahorro. Una brújula, no una regla estricta.",
            whyItMatters = "Te da una referencia rápida para saber si tu mes está equilibrado, sin llevar mil cuentas.",
            inTheApp = "Compara en Análisis tus gastos esenciales/elección y tu ahorro frente a este reparto.",
            rule = "Ajústalo a tu realidad: lo importante es tener una proporción, no estos números exactos.",
            example = "Con 1.500 al mes: ~750 a lo esencial, ~450 a tus gustos y ~300 para ti. Una brújula, no una jaula.",
            action = HelpAction.STATS,
            alsoKnownAs = "regla 50/30/20",
            visual = LessonVisual.SPLIT_50_30_20,
        ),
        Lesson(
            emoji = "🗓️",
            title = "Gasta a tu ritmo",
            oneLiner = "Saber cuánto te queda por día evita el susto de fin de mes.",
            whatItIs = "Repartir lo que te queda entre los días que faltan te da un ritmo diario cómodo.",
            whyItMatters = "Gastar mucho a principio de mes deja el final cuesta arriba. Un ritmo constante da calma.",
            inTheApp = "Cuando definas tu plan mensual, el inicio te mostrará tu ritmo sugerido por día.",
            rule = "Si un día te pasas, no pasa nada: compensa los siguientes.",
            example = "Te quedan 600 y faltan 20 días → unos 30 al día. Así llegas a fin de mes sin sustos.",
        ),
        Lesson(
            emoji = "⚙️",
            title = "Dinero que trabaja por ti",
            oneLiner = "Distingue el dinero por tu tiempo del que llega solo.",
            whatItIs = "El ingreso activo lo cambias por tu tiempo (sueldo, freelance). El pasivo llega sin tu tiempo (alquiler, intereses).",
            whyItMatters = "La meta a largo plazo de cualquier técnica es que una parte de tu dinero no dependa de tu tiempo.",
            inTheApp = "Al registrar un ingreso, eliges si es activo o pasivo. Así puedes ver crecer el pasivo.",
            rule = "Celebra cada ingreso pasivo, por pequeño que sea: es semilla.",
            example = "Tu sueldo es activo: lo cambias por tu tiempo. 20 de intereses que entran solos son pasivos: ahí empieza.",
            action = HelpAction.QUICK_ADD,
            alsoKnownAs = "ingreso activo vs pasivo",
        ),
        Lesson(
            emoji = "🛟",
            title = "Tu colchón para imprevistos",
            oneLiner = "Un ahorro que convierte una crisis en un mal rato.",
            whatItIs = "Es un fondo de entre 3 y 6 meses de tus gastos esenciales, guardado para emergencias.",
            whyItMatters = "Con colchón, un imprevisto (avería, mes sin ingresos) es un inconveniente, no una catástrofe.",
            inTheApp = "La app conoce tus gastos esenciales del mes: multiplícalos por 3–6 y ese es tu objetivo.",
            rule = "Es la prioridad número uno, antes de pensar en invertir.",
            example = "Si vivir te cuesta 1.000 al mes, tu colchón está entre 3.000 y 6.000. Llegas poco a poco, sin prisa.",
            action = HelpAction.STATS,
            alsoKnownAs = "fondo de emergencia",
        ),
        Lesson(
            emoji = "📓",
            title = "Anota cada gasto, sin esfuerzo",
            oneLiner = "El simple acto de anotar ya te hace gastar con más cabeza.",
            whatItIs = "Un método japonés de ahorro basado en escribir cada gasto a mano para hacerlo consciente.",
            whyItMatters = "No hace falta el cuaderno: registrar en la app cumple lo mismo y te da los números hechos.",
            inTheApp = "Cada movimiento que añades en el registro rápido ya es tu cuaderno digital.",
            rule = "Anota en el momento, no de memoria a fin de mes.",
            action = HelpAction.QUICK_ADD,
            alsoKnownAs = "método Kakebo",
        ),
    ),
)

/** §5 extended — the full operational flow, for whoever wants the detail. */
private val theFullFlow = LessonLibrary(
    title = "El flujo completo, paso a paso",
    subtitle = "Cómo se mueve el dinero dentro de la app.",
    lessons = listOf(
        Lesson(
            emoji = "💙",
            title = "El dinero entra",
            oneLiner = "Registra cada ingreso el día que llega.",
            whatItIs = "Anota lo que recibes como ingreso y di si es por tu tiempo (activo) o sin él (pasivo).",
            whyItMatters = "Registrar a tiempo mantiene tus números reales; de memoria a fin de mes, se escapan cosas.",
            inTheApp = "Registro rápido → tipo Ingreso. El ingreso se ve en azul en toda la app.",
            rule = "El día que entra, se anota.",
            action = HelpAction.QUICK_ADD,
        ),
        Lesson(
            emoji = "🫙",
            title = "Aparta el ahorro",
            oneLiner = "Tu primer movimiento del mes, tras cobrar.",
            whatItIs = "Separas una parte como ahorro antes de empezar a gastar.",
            whyItMatters = "Es la forma más simple y eficaz de ahorrar de verdad.",
            inTheApp = "Registro rápido → tipo Ahorro. No es ingreso ni gasto: tiene su propio color.",
            rule = "Primer movimiento tras la nómina = un ahorro.",
            action = HelpAction.QUICK_ADD,
        ),
        Lesson(
            emoji = "🏷️",
            title = "Clasifica bien (vale 10 segundos)",
            oneLiner = "La categoría es lo que convierte tus gastos en información útil.",
            whatItIs = "Cada categoría ya sabe si es esencial o elección, si se repite o cambia, y si te devuelve algo.",
            whyItMatters = "Sin clasificar tienes una lista; clasificando, tienes un diagnóstico de tu mes.",
            inTheApp = "Revisa y ajusta tus categorías en cualquier momento.",
            rule = "Piensa la categoría una vez; luego se cuida sola.",
            action = HelpAction.CATEGORIES,
        ),
        Lesson(
            emoji = "⏱️",
            title = "Lo que se repite, automatízalo",
            oneLiner = "Nómina, alquiler, suscripciones: que se anoten solos.",
            whatItIs = "Los movimientos recurrentes se registran por ti según la frecuencia que definas.",
            whyItMatters = "Menos fricción, cero olvidos, y tus números siempre al día.",
            inTheApp = "Gestiona los recurrentes desde Ajustes.",
            rule = "Todo gasto fijo es candidato a recurrente.",
        ),
    ),
)

/** §4.3 — the three opt-in libraries, in order. */
val lessonLibraries: List<LessonLibrary> = listOf(readYourNumbers, techniques, theFullFlow)
