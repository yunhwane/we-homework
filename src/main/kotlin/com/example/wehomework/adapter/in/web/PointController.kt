package com.example.wehomework.adapter.`in`.web

import com.example.wehomework.adapter.`in`.web.request.ApplyPointRequest
import com.example.wehomework.application.port.`in`.ApplyPointCommand
import com.example.wehomework.application.port.`in`.ApplyPointUseCase
import com.example.wehomework.domain.ApplyPointResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/point")
class PointController(
    private val applyPointUseCase: ApplyPointUseCase
) {
    
    @PostMapping("/apply")
    fun applyPoint(@RequestBody request: ApplyPointRequest): Mono<ResponseEntity<ApiResponse<ApplyPointResult>>> {
        val command = ApplyPointCommand(
            userId = request.userId
        )
        
        return applyPointUseCase.apply(command)
            .map { result ->
                ResponseEntity.ok(
                    ApiResponse.success(
                        data = result,
                        message = "포인트 신청이 완료되었습니다."
                    )
                )
            }
    }
}