package com.example.domain.ai

import com.example.data.model.AiPackagingInspection

interface AiInspectionRepository {
    suspend fun inspectProductImages(
        imageUriStrings: List<String>,
        notes: String = ""
    ): Result<AiPackagingInspection>
}
