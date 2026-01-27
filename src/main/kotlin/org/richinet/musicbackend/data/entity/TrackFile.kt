package org.richinet.musicbackend.data.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "TrackFile")
class TrackFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FileId")
    var fileId: Long? = null

    @Column(name = "TrackId")
    var trackId: Long? = null

    @Column(name = "FileName")
    var fileName: String? = null

    @Column(name = "FileLocation")
    var fileLocation: String? = null

    @Column(name = "FileOnline")
    var fileOnline: String? = null

    @Column(name = "Duration")
    var duration: BigDecimal? = null

    @Column(name = "BackupDate")
    var backupDate: Timestamp? = null

    @ManyToOne
    @JoinColumn(name = "TrackId", insertable = false, updatable = false)
    var track: Track? = null
}
