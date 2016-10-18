/**
 * Copyright 2016 Shu Tadaka.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package net.east301.sbcll

import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.HashMap
import java.util.ServiceLoader


/**
 * Collects [Command]s, and parses command line arguments, and runs the specified command.
 *
 * @author Shu Tadaka
 */
open class Launcher {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            main(System.getProperty("sun.java.command") ?: "(unknown)", args)
        }

        @JvmStatic
        fun main(name: String, args: Array<String>) {
            Launcher().run(name, args)
        }

    }

    /**
     * Runs the application.
     *
     * @param name  program name
     * @param args  command line arguments
     */
    fun run(name: String, args: Array<String>) {
        //
        val parser = ArgumentParsers.newArgumentParser("")
        val subparsers = parser.addSubparsers().dest("_command")

        val commands = HashMap<String, Command>()
        ServiceLoader.load(Command::class.java).forEach { command ->
            command.configureCommandLineParser(subparsers).forEach {
                commands[it] = command
            }
        }

        //
        val parsedArgs: Namespace
        try {
            parsedArgs = parser.parseArgs(args)
        } catch (_: ArgumentParserException) {
            parser.printHelp()
            return
        }

        //
        val command = commands[parsedArgs.getString("_command")] ?: throw RuntimeException("Invalid state")

        val builder = SpringApplicationBuilder()
        val properties = HashMap<String, Any>()

        ServiceLoader.load(ApplicationEnvironment::class.java).forEach {
            it.configureApplicationBuilder(builder, parsedArgs)
            properties.putAll(it.getApplicationProperties(parsedArgs))
        }

        command.configureApplicationBuilder(builder, parsedArgs)
        properties.putAll(command.getApplicationProperties(parsedArgs))

        //
        ApplicationPropertySetter.registerProperties(properties)

        val applicationContext = builder.run()
        command.run(applicationContext, parsedArgs)
    }

}
