import java.math.BigDecimal
import java.math.MathContext
import java.util.*

/**
 * The class `MulticurrencyWallet` performs the functionality
 * of multicurrency wallet.
 *
 * @see [MulticurrencyWalletException]
 * @author Andrian Rabysh
 */
class MulticurrencyWallet {
    /**
     * The first added currency is used as a default currency.
     */
    var defaultCurrency: String? = null
        private set
    private val balanceInCurrency: MutableMap<String, BigDecimal> = HashMap()
    private val rateOfCurrencies: MutableMap<Pair<String, String>, Pair<Int, Int>> = HashMap()

    /**
     * Adds new currency.
     * Sets rate 1 to 1 for (currency:currency) exchange pair.
     *
     * @param currency to add.
     */
    fun addCurrency(currency: String) {
        if (defaultCurrency == null) {
            defaultCurrency = currency
        }
        balanceInCurrency[currency] = BigDecimal.ZERO
        pushRateToMap(currency, currency, 1, 1)
    }

    /**
     * Deposits amount to provided or default currency.
     *
     * @param amount to deposit.
     * @param currency is an optional, if not provided [defaultCurrency] is used.
     * @throws IllegalArgumentException when amount not positive.
     * @throws MulticurrencyWalletException when currency wasn't added using [addCurrency].
     */
    fun deposit(amount: Double, currency: String? = defaultCurrency) {
        checkAmount(amount)
        checkCurrencyValid(currency)
        changeBalance(amount.toBigDecimal(), currency!!, true)
    }

    /**
     * Withdraws amount from provided or default currency.
     *
     * @param amount to withdraw.
     * @param currency is an optional, if not provided [defaultCurrency] is used.
     * @throws IllegalArgumentException when amount aren't positive.
     * @throws MulticurrencyWalletException when currency wasn't added using [addCurrency].
     */
    fun withdraw(amount: Double, currency: String? = defaultCurrency) {
        checkAmount(amount)
        checkCurrencyValid(currency)
        val amountBD = amount.toBigDecimal()
        changeBalance(amountBD, currency!!, false)
    }

    /**
     * Sets the exchange rate for a pair of currencies.
     *
     * @param currency1 is a base currency.
     * @param currency2 is a quote currency.
     * @param rate1 is a rate for base currency.
     * @param rate2 is a rate for quote currency.
     * @throws IllegalArgumentException when rateies aren't positive.
     * @throws MulticurrencyWalletException when trying change the rate within the same currency.
     */
    fun setRate(currency1: String, currency2: String, rate1: Int, rate2: Int) {
        if (rate1 <= 0 || rate2 <= 0) {
            throw IllegalArgumentException("rateies expected to be positive")
        }
        checkCurrencyValid(currency1)
        checkCurrencyValid(currency2)
        if (currency1 == currency2) {
            throw MulticurrencyWalletException("Cannot change the rate within the same currency")
        }

        pushRateToMap(currency1, currency2, rate1, rate2)
    }

    /**
     * Converts an amount of the base currency to the quote currency
     * based on existing rates.
     *
     * @param amount of base currency to convert.
     * @param currency1 base currency.
     * @param currency2 quote currency.
     * @throws IllegalArgumentException when amount isn't positive.
     * @throws MulticurrencyWalletException when currencies rate wasn't added using [setRate].
     */
    fun convert(amount: Double, currency1: String, currency2: String) {
        checkAmount(amount)
        val amountBD = amount.toBigDecimal()
        val convertedAmount = convertAmount(amountBD, currency1, currency2)
        changeBalance(amountBD, currency1, false)
        changeBalance(convertedAmount, currency2, true)
    }

    /**
     * Returns all pairs of balance and currency.
     *
     * @return string as "`balance` `currency`" joined with line separator.
     * @throws MulticurrencyWalletException when no currencies added with [addCurrency].
     */
    fun showBalance(): String {
        if (balanceInCurrency.isEmpty()) {
            throw MulticurrencyWalletException("No currency in wallet")
        }
        return balanceInCurrency.entries.joinToString(System.lineSeparator()) { (c, b) -> showBalanceInCurrency(b, c) }
    }

    /**
     * Returns a pair of total balance in provided or default currency and currency itself.
     *
     * @param currency is an optional, if not provided [defaultCurrency] is used.
     * @return string as "`balance` `curency`".
     * @throws MulticurrencyWalletException when currency wasn't added using [addCurrency].
     */
    fun showTotal(currency: String? = defaultCurrency): String {
        checkCurrencyValid(currency)
        val totalBalance =
            balanceInCurrency.entries.map { (cur, balance) -> convertAmount(balance, cur, currency!!) }
                .fold(BigDecimal.ZERO) { t, c -> t + c }
        return showBalanceInCurrency(totalBalance, currency!!)
    }

    private fun checkCurrencyValid(currency: String?) {
        if (!balanceInCurrency.containsKey(currency)) {
            throw MulticurrencyWalletException("${currency ?: "Default"} currency not found")
        }
    }

    private fun checkAmount(amount: Double) {
        if (amount <= 0) {
            throw IllegalArgumentException("Amount expected be positive")
        }
    }
    
    private fun pushRateToMap(currency1: String, currency2: String, rate1: Int, rate2: Int) {
        rateOfCurrencies.apply {
            set(Pair(currency1, currency2), Pair(rate1, rate2))
            set(Pair(currency2, currency1), Pair(rate2, rate1))
        }
    }

    private fun changeBalance(amount: BigDecimal, currency: String, isAdd: Boolean) {
        if (!isAdd) {
            if (balanceInCurrency[currency]!! < amount) {
                throw MulticurrencyWalletException("Not enough money in $currency currency")
            }
        }
        balanceInCurrency[currency] = balanceInCurrency[currency]!! + (if (isAdd) 1 else -1).toBigDecimal() * amount
    }

    private fun convertAmount(amount: BigDecimal, currency1: String, currency2: String): BigDecimal {
        val rate: Pair<Int, Int> = rateOfCurrencies[Pair(currency1, currency2)]
            ?: throw MulticurrencyWalletException("Rate of $currency1 to $currency2 not found")
        return (amount * rate.second.toBigDecimal()).divide(rate.first.toBigDecimal(), MathContext.DECIMAL128)
    }

    private fun showBalanceInCurrency(balance: BigDecimal, currency: String) = "${
        String.format(Locale.ROOT, "%.2f", balance)
            .trimEnd('0')
            .trimEnd('.')
    } $currency"
}
