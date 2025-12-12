package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ModuleProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        // Debug log the options map so we can see what KSP passed in this invocation
        println("ModuleProvider.create() environment.options = ${environment.options}")
        println("ModuleProvider.create() environment.options.keys = ${environment.options.keys}")
        return ModuleProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options,
        )
    }
}
