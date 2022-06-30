fun main() {
    Parser.printHello()
    Parser.printMan()
    val parser = Parser(MulticurrencyWallet())
    var line: String? = readLine()
    while (line != null) {
        if (line.isNotEmpty()) {
            try {
                parser.parse(line)
            } catch (ex: Exception) {
                when (ex) {
                    is IllegalArgumentException, is MulticurrencyWalletException -> {
                        println(ex.message)
                        println("Try to enter again")
                    }
                    else -> throw ex
                }
            }
        }
        line = readLine()
    }
}
