package com.pharos.app

import android.content.Context
import com.pharos.app.data.db.PharosDatabase
import com.pharos.app.data.repository.AnalysisRepository
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.data.repository.FolderRepository
import com.pharos.app.data.repository.ProjectRepository
import com.pharos.app.data.repository.SettingsRepository
import com.pharos.app.domain.usecase.AnalysisUseCase
import com.pharos.app.domain.usecase.MasterfileUseCase
import com.pharos.app.domain.usecase.ProjectClusteringUseCase
import com.pharos.app.domain.usecase.ScanUseCase
import com.pharos.app.network.api.AiApiProvider
import com.pharos.app.network.api.PerplexityApiProvider
import com.pharos.app.util.PdfTextExtractor
import com.pharos.app.util.TextExtractor

/**
 * Simple manual dependency injection container.
 */
class AppContainer(context: Context) {

    private val database = PharosDatabase.getInstance(context)

    val settingsRepository = SettingsRepository(context)

    val folderRepository = FolderRepository(database.folderDao())

    val fileRepository = FileRepository(database.fileDao())

    val analysisRepository = AnalysisRepository(database.analysisDao())

    val projectRepository = ProjectRepository(
        database.projectDao(),
        database.projectFileCrossRefDao()
    )

    val textExtractor: TextExtractor = TextExtractor(context)

    val pdfTextExtractor: PdfTextExtractor = PdfTextExtractor()

    val aiApiProvider: AiApiProvider = PerplexityApiProvider()

    val scanUseCase = ScanUseCase(
        context = context,
        folderRepository = folderRepository,
        fileRepository = fileRepository,
        textExtractor = textExtractor,
        pdfTextExtractor = pdfTextExtractor
    )

    val analysisUseCase = AnalysisUseCase(
        fileRepository = fileRepository,
        analysisRepository = analysisRepository,
        settingsRepository = settingsRepository,
        aiApiProvider = aiApiProvider,
        textExtractor = textExtractor,
        pdfTextExtractor = pdfTextExtractor,
        context = context
    )

    val projectClusteringUseCase = ProjectClusteringUseCase(
        analysisRepository = analysisRepository,
        projectRepository = projectRepository,
        fileRepository = fileRepository
    )

    val masterfileUseCase = MasterfileUseCase(
        context = context,
        projectRepository = projectRepository,
        fileRepository = fileRepository,
        analysisRepository = analysisRepository,
        folderRepository = folderRepository
    )
}
