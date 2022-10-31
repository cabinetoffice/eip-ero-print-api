package uk.gov.dluhc.printapi.rest;

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import java.net.URL
import java.util.Vector

class SshClient(
    private val username: String,
    privateKeyResourceUrl: URL,
    private val host: String,
    private val port: Int,
    private val sessionTimeout: Int = 10000,
    private val channelTimeout: Int = 5000,
) {
    private val jsch: JSch = JSch()

    init {
        jsch.addIdentity(privateKeyResourceUrl.path)
    }

    companion object {
        fun createLsCommand(targetDirectory: String = ""): (ChannelSftp) -> Vector<ChannelSftp.LsEntry>  =
            { it.ls(targetDirectory) as Vector<ChannelSftp.LsEntry> }
    }

    fun <R> createSessionAndExecute(cmd: (ChannelSftp) -> R): R {
        val runCommandResponse: R
        jsch.getSession(username, host, port).run {
            try {
                setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
                setConfig("StrictHostKeyChecking", "no")
                connect(sessionTimeout)
                val sftp: ChannelSftp = openChannel("sftp") as ChannelSftp
                try {
                    sftp.connect(channelTimeout)
                    runCommandResponse = cmd.invoke(sftp)
                } finally {
                    sftp.exit()
                }
            } finally {
                disconnect()
            }
        }
        return runCommandResponse!!
    }
}
