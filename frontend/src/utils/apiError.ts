import axios from "axios";

export function getApiErrorMessage(error: unknown, fallback = "Something went wrong") {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data as { message?: string; errors?: Record<string, string> } | undefined;
    if (responseData?.message) {
      if (responseData.errors && Object.keys(responseData.errors).length) {
        const details = Object.values(responseData.errors).join(", ");
        return `${responseData.message}: ${details}`;
      }
      return responseData.message;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
