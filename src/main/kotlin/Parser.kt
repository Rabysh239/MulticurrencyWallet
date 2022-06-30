import java.lang.reflect.InvocationTargetException
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.reflect.KFunction

/**
 * Parses user queries and invokes multicurrency wallet's functions based on it.
 *
 * @property wallet is a [MulticurrencyWallet] to which queries are addressed.
 * @see [MulticurrencyWallet]
 * @author Rabysh Andrian
 */
class Parser(private val wallet: MulticurrencyWallet) {
    companion object {
        private val sep = System.lineSeparator()
        private val hello =
            "=*".repeat(8) + " Мультивалютный кошелёк " + "=*".repeat(8) + sep +
                    "\t".repeat(5) + "Добро пожаловать!$sep"

        private const val man =
            """Использование: операция из списка, где <> - обязательные параметры, [] - опциональные
[currency] - если не указано в запросе использовать первую добавленную в кошелёк валюту
Операции:
    add currency <currency>
    deposit <amount> [currency]
    withdraw <amount> [currency]
    set rate <currency1> <currency2> <part1>:<part2>
    convert <amount> <currency1> to <currency2>
    show balance
    show total [in currency]"""

        /**
         * Prints greeting.
         */
        fun printHello() = println(hello)

        /**
         * Prints manual.
         */
        fun printMan() = println(man)
    }

    private val map = mapOf(
        pushToMap("^add currency \\w+$", MulticurrencyWallet::addCurrency, 2),
        pushToMap("^deposit -?\\d+(.\\d+)?( \\w+)?$", MulticurrencyWallet::deposit, 1, 2),
        pushToMap("^withdraw -?\\d+(.\\d+)?( \\w+)$", MulticurrencyWallet::withdraw, 1, 2),
        pushToMap("^set rate -?\\w+ \\w+ \\d+:\\d+$", MulticurrencyWallet::setRate, 2, 3, 4, 5),
        pushToMap("^convert -?\\d+(.\\d+)? \\w+ to \\w+$", MulticurrencyWallet::convert, 1, 2, 4),
        pushToMap("^show balance$", MulticurrencyWallet::showBalance),
        pushToMap("^show total( in \\w+)?$", MulticurrencyWallet::showTotal, 3),
    )

    /**
     * Splits user-input into tokens and invokes multicurrency wallet's functions based on it.
     *
     * @param str is a query to parse.
     * @throws IllegalArgumentException when no pattern matches query or it is handled from [MulticurrencyWallet].
     * @throws MulticurrencyWalletException when it is handled from [MulticurrencyWallet].
     * @throws IllegalStateException not expected to be thrown.
     */
    fun parse(str: String) {
        if (str == "-h" || str == "--help") {
            printMan()
            return
        }
        val filteredMap = map.filter { it.key.test(str) }
        if (filteredMap.isEmpty()) {
            throw IllegalArgumentException("No pattern matches query")
        }
        filteredMap.values.first().run {
            val parsedArgs = str.split(" ", ":")
            val args = first.takeWhile { it < parsedArgs.size }.map(parsedArgs::get)
                .mapIndexed<String, Any> { i, e ->
                    when (second.parameters[i + 1].type.classifier) {
                        Int::class -> e.toInt()
                        Double::class -> e.toDouble()
                        String::class -> e
                        else -> throw IllegalStateException("Unexpected function parameter")
                    }
                }.toTypedArray()
            try {
                val result = if (second.parameters.size == 1 + args.size) {
                    second.call(wallet, *args)
                } else {
                    second.call(wallet, *args, wallet.defaultCurrency)
                }
                if (result is String) {
                    println(result)
                }
            } catch (ex: InvocationTargetException) {
                when (ex.cause) {
                    is MulticurrencyWalletException ->
                        throw MulticurrencyWalletException((ex.cause as MulticurrencyWalletException).message ?: "Command is invalid")
                    is IllegalArgumentException ->
                        throw IllegalArgumentException((ex.cause as IllegalArgumentException).message)
                    else -> throw ex.cause ?: throw IllegalArgumentException("Exception while parsing your input. Please retry")
                }
            }
        }
    }

    private fun <R> pushToMap(regex: String, fn: KFunction<R>, vararg list: Int):
            Pair<Predicate<String>, Pair<IntArray, KFunction<R>>> =
        Pair(Pattern.compile(regex).asPredicate(), Pair(list, fn))
}
