import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.pow

/**
 * Tests for multicurrency wallet.
 *
 * @see [MulticurrencyWallet]
 * @author Rabysh Andrian
 */
class MulticurrencyWalletTest {
    private lateinit var wallet: MulticurrencyWallet

    companion object {
        private const val RUBLE = "ruble"
        private const val DOLLAR = "dollar"
        private val sep = System.lineSeparator()
        private val MULTICURRENCY_EXCEPTION_CLASS = MulticurrencyWalletException::class.javaObjectType
    }

    @BeforeEach
    fun presetting() {
        wallet = MulticurrencyWallet()
    }

    @Test
    fun `No currency test`() {
        wallet.run {
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { deposit(1.0) }
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { withdraw(1.0) }
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { setRate(RUBLE, DOLLAR, 1, 1) }
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { convert(1.0, RUBLE, DOLLAR) }
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS, ::showTotal, ::showBalance)
        }
    }

    @Test
    fun `Add currency test`() {
        wallet.run {
            addCurrency(RUBLE)
            assertEquals("0 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Deposit test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(1.0)
            deposit(1.0, RUBLE)
            assertEquals("2 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Withdraw not enough test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(1.0)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { withdraw(2.0, RUBLE) }
            assertEquals("1 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Withdraw test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(3.0)
            withdraw(1.0)
            withdraw(1.0, RUBLE)
            assertEquals("1 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Rate 1 1 test`() {
        wallet.run {
            addCurrency(RUBLE)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { setRate(RUBLE, RUBLE, 1, 2) }
        }
    }

    @Test
    fun `No cheat convert test`() {
        wallet.run {
            addCurrency(RUBLE)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { convert(1.0, RUBLE, RUBLE) }
        }
    }

    @Test
    fun `Useless convert test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(1.0)
            convert(1.0, RUBLE, RUBLE)
            assertEquals("1 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Accuracy test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(.2)
            deposit(.1)
            assertEquals("0.3 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Big numbers test`() {
        wallet.run {
            addCurrency(RUBLE)
            (1..1_000_000).forEach { _ -> deposit(10.0.pow(10)) }
            assertEquals("10000000000000000 $RUBLE", showTotal(), showBalance())
        }
    }

    @Test
    fun `Add two currency`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            assertEquals("0 $RUBLE${sep}0 $DOLLAR", showBalance())
        }
    }

    @Test
    fun `Deposit with two currency`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            assertEquals("0 $RUBLE${sep}0 $DOLLAR", showBalance())
        }
    }

    @Test
    fun `Show total test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1, 1)
            deposit(1.0)
            deposit(1.0, DOLLAR)
            assertEquals("2 $RUBLE", showTotal())
            assertEquals("2 $DOLLAR", showTotal(DOLLAR))
        }
    }

    @Test
    fun `Undefined rate test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { showTotal() }
        }
    }

    @Test
    fun `Default currency not changes test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1, 1)
            assertEquals("0 $RUBLE", showTotal())
        }
    }

    @Test
    fun `Convert test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1, 2)
            deposit(1.0, RUBLE)
            convert(1.0, RUBLE, DOLLAR)
            assertEquals("2 $DOLLAR", showTotal(DOLLAR))
        }
    }

    @Test
    fun `Convert test 2`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1, 2)
            deposit(1.0, RUBLE)
            convert(1.0, RUBLE, DOLLAR)
            convert(2.0, DOLLAR, RUBLE)
            assertEquals("1 $RUBLE", showTotal(RUBLE))
        }
    }

    @Test
    fun `Convert test 3`() {
        val euro = "euro"
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            addCurrency(euro)
            setRate(RUBLE, DOLLAR, 1, 2)
            setRate(DOLLAR, euro, 1, 2)
            setRate(RUBLE, euro, 1, 1)
            deposit(1.0, RUBLE)
            convert(1.0, RUBLE, DOLLAR)
            convert(2.0, DOLLAR, euro)
            assertEquals("4 $euro", showTotal(euro))
        }
    }

    @Test
    fun `Rate test`() {
        val euro = "euro"
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            addCurrency(euro)
            setRate(RUBLE, DOLLAR, 1, 2)
            setRate(DOLLAR, euro, 1, 2)
            deposit(1.0, RUBLE)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { convert(1.0, RUBLE, euro) }
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { showTotal(euro) }
        }
    }

    @Test
    fun `Rate changes in runtime test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            deposit(1.0, RUBLE)
            setRate(RUBLE, DOLLAR, 1, 2)
            convert(1.0, RUBLE, DOLLAR)
            setRate(DOLLAR, RUBLE, 1, 2)
            convert(1.0, DOLLAR, RUBLE)
            assertEquals("4 $RUBLE", showTotal(RUBLE))
        }
    }

    @Test
    fun `Multiconvert test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1000, 1)
            (1..10000).forEach { _ ->
                deposit(100.0)
                convert(100.0, RUBLE, DOLLAR)
            }
            assertEquals("1000 $DOLLAR", showTotal(DOLLAR))
        }
    }

    @Test
    fun `Cheat rate test`() {
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            setRate(RUBLE, DOLLAR, 1, 2)
            setRate(DOLLAR, RUBLE, 1, 2)
            deposit(1.0, RUBLE)
            convert(1.0, RUBLE, DOLLAR)
            assertThrows(MULTICURRENCY_EXCEPTION_CLASS) { convert(2.0, DOLLAR, RUBLE) }
            assertEquals("1 $RUBLE", showTotal())
        }
    }

    @Test
    fun `Becoming wealthier`() {
        val euro = "euro"
        wallet.run {
            addCurrency(RUBLE)
            addCurrency(DOLLAR)
            addCurrency(euro)
            setRate(RUBLE, DOLLAR, 1, 2)
            setRate(DOLLAR, euro, 1, 2)
            setRate(euro, RUBLE, 1, 2)
            deposit(1.0, RUBLE)
            convert(1.0, RUBLE, DOLLAR)
            convert(2.0, DOLLAR, euro)
            convert(4.0, euro, RUBLE)
            assertEquals("8 $RUBLE", showTotal())
        }
    }

    @Test
    fun `Example test`() {
        wallet.run {
            addCurrency(RUBLE)
            deposit(100.0)
            addCurrency(DOLLAR)
            setRate(DOLLAR, RUBLE, 1, 60)
            deposit(1.0, DOLLAR)
            convert(80.0, RUBLE, DOLLAR)
            withdraw(2.1, DOLLAR)
            assertEquals("20 $RUBLE${sep}0.23 $DOLLAR", showBalance())
            assertEquals("34 $RUBLE", showTotal())
            assertEquals("0.57 $DOLLAR", showTotal(DOLLAR))
        }
    }
}
