package at.hannibal2.skyhanni.features.tutorial.handlers

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import kotlin.reflect.KClass

/**
 * Registry for tutorial fork handlers.
 */
object TutorialForkHandlerRegistry {
    
    private val forkHandlers = mutableMapOf<KClass<out TutorialFork>, TutorialForkHandler<*>>()
    
    /**
     * Register a handler for a specific fork type
     */
    fun <T : TutorialFork> registerForkHandler(forkClass: KClass<T>, handler: TutorialForkHandler<T>) {
        forkHandlers[forkClass] = handler
    }
    
    /**
     * Get handler for a specific fork
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : TutorialFork> getHandler(fork: T): TutorialForkHandler<T>? {
        return forkHandlers[fork::class] as? TutorialForkHandler<T>
    }
    
    /**
     * Get nodes using registered handler
     */
    fun getNodes(fork: TutorialFork, tutorial: Tutorial): List<TutorialNode> {
        val handler = getHandler(fork)
        return handler?.getNodes(fork, tutorial) ?: emptyList()
    }
    
    /**
     * Get all internal nodes using registered handler
     */
    fun getAllInternalNodes(fork: TutorialFork): List<TutorialNode> {
        val handler = getHandler(fork)
        return handler?.getAllInternalNodes(fork) ?: emptyList()
    }
    
    /**
     * Check if fork is async
     */
    fun isAsync(fork: TutorialFork): Boolean {
        val handler = getHandler(fork)
        return handler?.isAsync(fork) ?: false
    }
    
    /**
     * Get header using registered handler
     */
    fun getHeader(fork: TutorialFork, tutorial: Tutorial): String {
        val handler = getHandler(fork)
        return handler?.getHeader(fork, tutorial) ?: "Fork"
    }
    
    /**
     * Check if complete using registered handler
     */
    fun isComplete(fork: TutorialFork, tutorial: Tutorial): Boolean {
        val handler = getHandler(fork)
        return handler?.isComplete(fork, tutorial) ?: false
    }
    
    /**
     * Initialize all handlers
     */
    fun init() {
        // Handlers will be registered here
    }
}
